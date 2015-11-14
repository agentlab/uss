/*
 * Copyright (c) 2015 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.userstorage.internal;

import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.JSONUtil;
import org.eclipse.userstorage.internal.util.ProxyUtil;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.spi.ICredentialsProvider;
import org.eclipse.userstorage.util.ConflictException;
import org.eclipse.userstorage.util.ProtocolException;

import org.eclipse.core.runtime.OperationCanceledException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public class Session implements Headers, Codes
{
  public static final String APPLICATION_JSON = "application/json";

  public static final String USER_AGENT_ID = "uss/1.0.0"; // "uss/1.0.0" or use bundle version if running in OSGi...

  public static final String USER_AGENT_PROPERTY = Session.class.getName() + ".userAgent";

  private static final int AUTHORIZATION_ATTEMPTS = 3;

  private static final boolean DEBUG = Boolean.getBoolean(Session.class.getName() + ".debug");

  /**
   * It's important that the cookie store is <b>not</b> declared as a static field!
   * Otherwise session cookies could be left over even if the sessionID is set to null and
   * re-authentication would re-send old session cookies, which would make the server reply with "401: CSRF Validation Failed".
   */
  @SuppressWarnings("restriction")
  private final CookieStore cookieStore = new org.apache.http.impl.client.BasicCookieStore();

  private final Storage storage;

  private final Executor executor;

  private String sessionID;

  private String csrfToken;

  public Session(Storage storage)
  {
    this.storage = storage;
    executor = Executor.newInstance().cookieStore(cookieStore);
  }

  public IStorage getStorage()
  {
    return storage;
  }

  public InputStream retrieveBlob(String appToken, String key, final Map<String, String> properties, final boolean useETag,
      ICredentialsProvider credentialsProvider) throws IOException
  {
    URI uri = StringUtil.newURI(storage.getServiceURI(), "api/blob/" + appToken + "/" + key);

    return new RequestTemplate<InputStream>(uri)
    {
      @Override
      protected Request prepareRequest() throws IOException
      {
        Request request = configureRequest(Request.Get(uri), uri);

        if (useETag)
        {
          String eTag = properties.get(Blob.ETAG);
          if (!StringUtil.isEmpty(eTag))
          {
            request.setHeader(IF_NON_MATCH, "\"" + eTag + "\"");
          }
        }

        return request;
      }

      @Override
      protected InputStream handleResponse(HttpResponse response, HttpEntity responseEntity) throws IOException
      {
        int statusCode = getStatusCode("GET", uri, response, OK, NOT_MODIFIED, NOT_FOUND);

        String eTag = getETag(response);
        if (eTag != null)
        {
          properties.put(Blob.ETAG, eTag);
        }

        if (statusCode == OK)
        {
          return JSONUtil.decode(responseEntity.getContent(), properties, "value");
        }

        if (statusCode == NOT_MODIFIED)
        {
          return Blob.NOT_MODIFIED;
        }

        // NOT_FOUND
        return null;
      }
    }.send(credentialsProvider);
  }

  public boolean updateBlob(String appToken, String key, final Map<String, String> properties, final InputStream in, ICredentialsProvider credentialsProvider)
      throws IOException, ConflictException
  {
    URI uri = StringUtil.newURI(storage.getServiceURI(), "api/blob/" + appToken + "/" + key);

    return new RequestTemplate<Boolean>(uri)
    {
      @Override
      protected Request prepareRequest() throws IOException
      {
        Request request = configureRequest(Request.Put(uri), uri);

        String eTag = properties.get(Blob.ETAG);
        if (!StringUtil.isEmpty(eTag))
        {
          request.setHeader(IF_MATCH, "\"" + eTag + "\"");
        }

        body = JSONUtil.encode(Blob.NO_PROPERTIES, "value", in);
        request.bodyStream(body);
        return request;
      }

      @Override
      protected Boolean handleResponse(HttpResponse response, HttpEntity responseEntity) throws IOException
      {
        int statusCode = getStatusCode("PUT", uri, response, OK, CREATED, CONFLICT);
        String eTag = getETag(response);

        if (statusCode == CONFLICT)
        {
          StatusLine statusLine = response.getStatusLine();
          throw new ConflictException("PUT", uri, getProtocolVersion(statusLine), statusLine.getReasonPhrase(), eTag);
        }

        if (eTag == null)
        {
          throw new ProtocolException("PUT", uri, getProtocolVersion(response.getStatusLine()), BAD_RESPONSE, "Bad Response : No ETag");
        }

        properties.put(Blob.ETAG, eTag);
        return statusCode == CREATED;
      }
    }.send(credentialsProvider);
  }

  private static String getETag(HttpResponse response)
  {
    Header[] headers = response.getHeaders(Headers.ETAG);
    if (headers != null && headers.length != 0)
    {
      String eTag = headers[0].getValue();

      // Remove the quotes.
      eTag = eTag.substring(1, eTag.length() - 1);

      return eTag;
    }

    return null;
  }

  /**
   * @author Eike Stepper
   */
  private abstract class RequestTemplate<T>
  {
    protected final URI uri;

    protected InputStream body;

    public RequestTemplate(URI uri)
    {
      this.uri = uri;
    }

    public final synchronized T send(ICredentialsProvider credentialsProvider) throws IOException
    {
      int authorizationAttempts = AUTHORIZATION_ATTEMPTS;

      for (;;)
      {
        body = null;
        HttpEntity responseEntity = null;

        try
        {
          authenticate(credentialsProvider);

          Request request = prepareRequest();
          HttpResponse response = sendRequest(request, uri);

          IOUtil.closeSilent(body);
          body = null;

          responseEntity = response.getEntity();
          return handleResponse(response, responseEntity);
        }
        catch (IOException ex)
        {
          if (DEBUG && responseEntity != null)
          {
            responseEntity.writeTo(System.out);
          }

          if (ex instanceof ProtocolException)
          {
            ProtocolException protocolException = (ProtocolException)ex;
            if (protocolException.getStatusCode() == AUTHORIZATION_REQUIRED && --authorizationAttempts > 0)
            {
              continue;
            }
          }

          throw ex;
        }
        finally
        {
          IOUtil.closeSilent(body);
          body = null;
        }
      }
    }

    protected final void authenticate(ICredentialsProvider credentialsProvider) throws IOException
    {
      if (sessionID == null)
      {
        // Make sure no old session session cookies are sent.
        // Otherwise the server would reply with "401: CSRF Validation Failed".
        cookieStore.clear();

        InputStream body = null;
        HttpEntity responseEntity = null;

        try
        {
          Credentials credentials = getCredentials(credentialsProvider);

          Map<String, String> arguments = new LinkedHashMap<String, String>();
          arguments.put("username", credentials.getUsername());
          arguments.put("password", credentials.getPassword());

          URI uri = StringUtil.newURI(storage.getServiceURI(), "api/user/login");

          Request request = configureRequest(Request.Post(uri), uri);
          body = JSONUtil.encode(arguments, null, null);
          request.bodyStream(body);

          HttpResponse response = sendRequest(request, uri);
          responseEntity = response.getEntity();

          getStatusCode("POST", uri, response, OK);

          Map<String, String> properties = new LinkedHashMap<String, String>();
          IOUtil.closeSilent(JSONUtil.decode(responseEntity.getContent(), properties, null));

          sessionID = properties.get("sessid");
          if (sessionID == null)
          {
            throw new IOException("No session ID");
          }

          csrfToken = properties.get("token");
        }
        catch (IOException ex)
        {
          sessionID = null;
          csrfToken = null;

          if (DEBUG && responseEntity != null)
          {
            responseEntity.writeTo(System.out);
          }

          throw ex;
        }
        finally
        {
          IOUtil.closeSilent(body);
        }
      }

      acquireCSRFToken();
    }

    protected final void acquireCSRFToken() throws IOException
    {
      if (csrfToken == null)
      {
        HttpEntity responseEntity = null;

        try
        {
          URI uri = StringUtil.newURI(storage.getServiceURI(), "api/user/token");

          Request request = configureRequest(Request.Post(uri), uri);
          HttpResponse response = sendRequest(request, uri);
          responseEntity = response.getEntity();

          Map<String, String> properties = new LinkedHashMap<String, String>();
          IOUtil.closeSilent(JSONUtil.decode(responseEntity.getContent(), properties, null));

          csrfToken = properties.get("token");
          if (csrfToken == null)
          {
            throw new IOException("No CSRF token");
          }
        }
        catch (IOException ex)
        {
          csrfToken = null;

          if (DEBUG && responseEntity != null)
          {
            responseEntity.writeTo(System.out);
          }

          throw ex;
        }
      }
    }

    protected final Request configureRequest(Request request, URI uri)
    {
      if (csrfToken != null)
      {
        request.setHeader(CSRF_TOKEN, csrfToken);
      }

      String userAgent = System.getProperty(USER_AGENT_PROPERTY, USER_AGENT_ID);

      return request //
          .viaProxy(ProxyUtil.getProxyHost(uri)) //
          .staleConnectionCheck(true) //
          .connectTimeout(StorageProperties.getProperty(StorageProperties.CONNECT_TIMEOUT, 3000)) //
          .socketTimeout(StorageProperties.getProperty(StorageProperties.SOCKET_TIMEOUT, 10000)) //
          .addHeader(USER_AGENT, userAgent) //
          .addHeader(CONTENT_TYPE, APPLICATION_JSON) //
          .addHeader(ACCEPT, APPLICATION_JSON);
    }

    protected final HttpResponse sendRequest(Request request, URI uri) throws IOException
    {
      long start = 0;
      if (DEBUG)
      {
        try
        {
          start = System.currentTimeMillis();
          StringBuilder builder = new StringBuilder();
          builder.append(request);
          builder.append('\n');

          Field f1 = Request.class.getDeclaredField("request");
          f1.setAccessible(true);
          Object o1 = f1.get(request);

          Field f2 = Class.forName("org.apache.http.message.AbstractHttpMessage").getDeclaredField("headergroup");
          f2.setAccessible(true);
          Object o2 = f2.get(o1);

          Field f3 = o2.getClass().getDeclaredField("headers");
          f3.setAccessible(true);
          @SuppressWarnings("unchecked")
          List<Header> o3 = (List<Header>)f3.get(o2);

          for (Header header : o3)
          {
            builder.append("   ");
            builder.append(header);
            builder.append('\n');
          }

          System.out.print(builder);
        }
        catch (Throwable ex)
        {
          ex.printStackTrace();
        }
      }

      Response result = ProxyUtil.proxyAuthentication(executor, uri).execute(request);
      HttpResponse response = result.returnResponse();

      if (DEBUG)
      {
        try
        {
          StringBuilder builder = new StringBuilder();
          builder.append(response.getStatusLine());
          builder.append('\n');

          for (Header header : response.getAllHeaders())
          {
            builder.append("   ");
            builder.append(header);
            builder.append('\n');
          }

          if (start != 0)
          {
            long millis = System.currentTimeMillis() - start;
            builder.append("Took: ");
            builder.append(millis);
            builder.append(" millis");
            builder.append('\n');
          }

          builder.append('\n');
          System.out.print(builder);
        }
        catch (Throwable ex)
        {
          ex.printStackTrace();
        }
      }

      return response;
    }

    protected final int getStatusCode(String method, URI uri, HttpResponse response, int... expectedStatusCodes) throws ProtocolException
    {
      StatusLine statusLine = response.getStatusLine();
      if (statusLine == null)
      {
        throw new ProtocolException(method, uri, getProtocolVersion(statusLine), BAD_RESPONSE, "Bad Response : No status line returned");
      }

      int statusCode = statusLine.getStatusCode();
      if (statusCode == AUTHORIZATION_REQUIRED)
      {
        sessionID = null;
        csrfToken = null;
      }

      for (int i = 0; i < expectedStatusCodes.length; i++)
      {
        int expectedStatusCode = expectedStatusCodes[i];
        if (statusCode == expectedStatusCode)
        {
          return statusCode;
        }
      }

      throw new ProtocolException(method, uri, getProtocolVersion(statusLine), statusCode, statusLine.getReasonPhrase());
    }

    protected final String getProtocolVersion(StatusLine statusLine)
    {
      if (statusLine != null)
      {
        ProtocolVersion protocolVersion = statusLine.getProtocolVersion();
        if (protocolVersion != null)
        {
          return protocolVersion.toString();
        }
      }

      return "HTTP";
    }

    protected final Credentials getCredentials(ICredentialsProvider credentialsProvider) throws OperationCanceledException
    {
      Credentials credentials = storage.getCredentials();
      if (credentials == null)
      {
        if (credentialsProvider != null)
        {
          credentials = credentialsProvider.provideCredentials(storage);
          if (credentials != null)
          {
            storage.setCredentials(credentials);
          }
        }
      }

      if (credentials == null)
      {
        throw new OperationCanceledException("No credentials provided");
      }

      return credentials;
    }

    protected abstract Request prepareRequest() throws IOException;

    protected abstract T handleResponse(HttpResponse response, HttpEntity responseEntity) throws IOException;
  }
}

/**
 * @author Eike Stepper
 */
interface Headers
{
  public static final String USER_AGENT = "User-Agent";

  public static final String CONTENT_TYPE = "Content-Type";

  public static final String ACCEPT = "Accept";

  public static final String CSRF_TOKEN = "X-CSRF-Token";

  public static final String ETAG = "ETag";

  public static final String IF_MATCH = "If-Match";

  public static final String IF_NON_MATCH = "If-Non-Match";
}

/**
 * @author Eike Stepper
 */
interface Codes
{
  public static final int OK = 200;

  public static final int CREATED = 201;

  public static final int NO_CONTENT = 204;

  public static final int NOT_MODIFIED = 304;

  public static final int BAD_REQUEST = 400;

  public static final int AUTHORIZATION_REQUIRED = 401;

  public static final int FORBIDDEN = 403;

  public static final int NOT_FOUND = 404;

  public static final int NOT_ACCEPTABLE = 406;

  public static final int CONFLICT = 409;

  public static final int BAD_RESPONSE = 444;
}
