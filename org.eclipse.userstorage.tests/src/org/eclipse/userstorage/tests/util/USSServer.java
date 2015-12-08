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
package org.eclipse.userstorage.tests.util;

import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.JSONUtil;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.spi.Credentials;
import org.eclipse.userstorage.tests.StorageTests;

import org.eclipse.core.runtime.Path;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Eike Stepper
 */
public final class USSServer
{
  public static final String BLOB_EXTENSION = ".blob";

  public static final String ETAG_EXTENSION = ".etag";

  private static final boolean DEBUG = Boolean.getBoolean("org.eclipse.userstorage.tests.server.debug");

  private final USSHandler handler = new USSHandler();

  private final Set<String> applicationTokens = new HashSet<String>();

  private final Map<String, User> users = new HashMap<String, User>();

  private final Map<String, Session> sessions = new HashMap<String, Session>();

  private final File folder;

  private final int startPort;

  private int port;

  private Server server;

  public USSServer(int startPort, File folder)
  {
    this.startPort = startPort;
    this.folder = folder;
  }

  public int getPort()
  {
    return port;
  }

  public File getFolder()
  {
    return folder;
  }

  public Set<String> getApplicationTokens()
  {
    return applicationTokens;
  }

  public Map<String, User> getUsers()
  {
    return users;
  }

  public User addUser(Credentials credentials)
  {
    return addUser(credentials.getUsername(), credentials.getPassword());
  }

  public User addUser(String username, String password)
  {
    User user = new User(username, password);
    users.put(user.getUsername(), user);
    return user;
  }

  public File getUserFile(User user, String applicationToken, String key, String extension)
  {
    return new File(getApplicationFolder(user, applicationToken), key + StringUtil.safe(extension));
  }

  public File getApplicationFolder(User user, String applicationToken)
  {
    return new File(new File(folder, user.getUsername()), applicationToken);
  }

  public Map<String, Session> getSessions()
  {
    return sessions;
  }

  public int start() throws Exception
  {
    Exception exception = new Exception("No free port");

    for (port = startPort; port < 65535; port++)
    {
      server = new Server(port);
      server.setHandler(handler);

      try
      {
        server.start();
        return port;
      }
      catch (Exception ex)
      {
        exception = ex;

        try
        {
          server.stop();
        }
        catch (Exception ignore)
        {
          //$FALL-THROUGH$
        }
        finally
        {
          server = null;
        }
      }
    }

    throw exception;
  }

  public void stop() throws Exception
  {
    if (server != null)
    {
      server.stop();
      server = null;
    }
  }

  public void join() throws InterruptedException
  {
    if (server != null)
    {
      server.join();
    }
  }

  protected void login(HttpServletRequest request, HttpServletResponse response) throws IOException
  {
    Map<String, Object> requestObject = JSONUtil.parse(request.getInputStream(), null);

    String username = (String)requestObject.get("username");
    String password = (String)requestObject.get("password");

    User user = users.get(username);
    if (user == null || password == null || !password.equals(user.getPassword()))
    {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");

    Session session = addSession(user);
    Cookie cookie = new Cookie("SESSION", session.getID());
    cookie.setPath("/");
    response.addCookie(cookie);

    Map<String, Object> responseObject = new LinkedHashMap<String, Object>();
    responseObject.put("sessid", session.getID());
    responseObject.put("token", session.getCSRFToken());
    InputStream body = JSONUtil.build(responseObject);

    try
    {
      ServletOutputStream out = response.getOutputStream();
      IOUtil.copy(body, out);
      out.flush();
    }
    finally
    {
      IOUtil.closeSilent(body);
    }
  }

  protected void retrieveProperties(HttpServletRequest request, HttpServletResponse response, File applicationFolder) throws IOException
  {
    String applicationToken = applicationFolder.getName();

    int pageSize = getIntParameter(request, "pageSize", 20);
    if (pageSize < 1 || pageSize > 100)
    {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid page size");
      return;
    }

    int page = getIntParameter(request, "page", 20);
    if (page < 1)
    {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid page");
      return;
    }

    boolean empty = true;

    StringBuilder builder = new StringBuilder();
    builder.append('[');

    File[] files = applicationFolder.listFiles();
    if (files != null)
    {
      int first = (page - 1) * pageSize + 1;
      System.out.println("##### " + first);
      int i = 0;

      for (File file : files)
      {
        String name = file.getName();
        if (name.endsWith(ETAG_EXTENSION))
        {
          if (++i >= first)
          {
            String key = name.substring(0, name.length() - ETAG_EXTENSION.length());
            System.out.println("##### " + key);
            String etag = IOUtil.readUTF(file);

            if (empty)
            {
              empty = false;
            }
            else
            {
              builder.append(",");
            }

            builder.append("{\"application_token\":\"");
            builder.append(applicationToken);
            builder.append("\",\"key\":\"");
            builder.append(key);
            builder.append("\",\"etag\":\"");
            builder.append(etag);
            builder.append("\"}");

            if (--pageSize == 0)
            {
              break;
            }
          }
        }
      }
    }

    builder.append(']');
    System.out.println(builder);

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");

    InputStream body = IOUtil.streamUTF(builder.toString());

    try
    {
      ServletOutputStream out = response.getOutputStream();
      IOUtil.copy(body, out);
      out.flush();
    }
    finally
    {
      IOUtil.closeSilent(body);
    }
  }

  protected void retrieveBlob(HttpServletRequest request, HttpServletResponse response, File blobFile, File etagFile, boolean exists) throws IOException
  {
    if (!exists)
    {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    String etag = IOUtil.readUTF(etagFile);
    String ifNoneMatch = getETag(request, "If-None-Match");
    if (ifNoneMatch != null && ifNoneMatch.equals(etag))
    {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");
    response.setHeader("ETag", "\"" + etag + "\"");

    InputStream body = JSONUtil.build(Collections.singletonMap("value", new FileInputStream(blobFile)));

    try
    {
      ServletOutputStream out = response.getOutputStream();
      IOUtil.copy(body, out);
      out.flush();
    }
    finally
    {
      IOUtil.closeSilent(body);
    }
  }

  protected void updateBlob(HttpServletRequest request, HttpServletResponse response, File blobFile, File etagFile, boolean exists) throws IOException
  {
    String ifMatch = getETag(request, "If-Match");

    if (exists)
    {
      String etag = IOUtil.readUTF(etagFile);

      if (StringUtil.isEmpty(ifMatch) || !ifMatch.equals(etag))
      {
        response.setHeader("ETag", "\"" + etag + "\"");
        response.sendError(HttpServletResponse.SC_CONFLICT);
        return;
      }
    }
    else
    {
      if (!StringUtil.isEmpty(ifMatch))
      {
        response.sendError(HttpServletResponse.SC_CONFLICT);
        return;
      }
    }

    String etag = UUID.randomUUID().toString();

    IOUtil.mkdirs(blobFile.getParentFile());
    FileOutputStream out = new FileOutputStream(blobFile);
    InputStream body = null;

    try
    {
      Map<String, Object> requestObject = JSONUtil.parse(request.getInputStream(), "value");
      body = (InputStream)requestObject.get("value");

      IOUtil.copy(body, out);
    }
    finally
    {
      IOUtil.closeSilent(body);
      IOUtil.close(out);
    }

    IOUtil.writeUTF(etagFile, etag);

    response.setStatus(exists ? HttpServletResponse.SC_OK : HttpServletResponse.SC_CREATED);
    response.setHeader("ETag", "\"" + etag + "\"");
  }

  protected void deleteBlob(HttpServletRequest request, HttpServletResponse response, File blobFile, File etagFile, boolean exists) throws IOException
  {
    if (exists)
    {
      String etag = IOUtil.readUTF(etagFile);
      String ifMatch = getETag(request, "If-Match");
      if (ifMatch != null && !ifMatch.equals(etag))
      {
        response.sendError(HttpServletResponse.SC_CONFLICT);
        return;
      }
    }
    else
    {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    IOUtil.delete(blobFile);
    IOUtil.delete(etagFile);

    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  private Session getSession(HttpServletRequest request)
  {
    String csrfToken = request.getHeader("X-CSRF-Token");
    if (csrfToken != null)
    {
      Cookie[] cookies = request.getCookies();
      if (cookies != null)
      {
        for (Cookie cookie : cookies)
        {
          if ("SESSION".equals(cookie.getName()))
          {
            String sessionID = cookie.getValue();
            Session session = sessions.get(sessionID);

            if (session != null && session.getCSRFToken().equals(csrfToken))
            {
              return session;
            }

            break;
          }
        }
      }
    }

    return null;
  }

  private Session addSession(User user)
  {
    Session session = new Session(user);
    sessions.put(session.getID(), session);
    return session;
  }

  private static String getETag(HttpServletRequest request, String headerName)
  {
    String eTag = request.getHeader(headerName);
    if (eTag != null)
    {
      // Remove the quotes.
      eTag = eTag.substring(1, eTag.length() - 1);
    }

    return eTag;
  }

  @SuppressWarnings("restriction")
  private static String getReasonPhrase(int status)
  {
    try
    {
      return org.apache.http.impl.EnglishReasonPhraseCatalog.INSTANCE.getReason(status, null);
    }
    catch (Throwable ex)
    {
      return "";
    }
  }

  private static int getIntParameter(HttpServletRequest request, String name, int defaultValue)
  {
    String parameter = request.getParameter(name);
    if (parameter != null)
    {
      try
      {
        return Integer.parseInt(parameter);
      }
      catch (NumberFormatException ex)
      {
        //$FALL-THROUGH$
      }
    }

    return defaultValue;
  }

  /**
   * @author Eike Stepper
   */
  private class USSHandler extends AbstractHandler
  {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
      try
      {
        baseRequest.setHandled(true);

        String userAgent = request.getHeader("User-Agent");
        if (!org.eclipse.userstorage.internal.Session.USER_AGENT_ID.equals(userAgent))
        {
          response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid User-Agent");
          return;
        }

        String method = request.getMethod();
        String path = request.getPathInfo();

        if (DEBUG)
        {
          StringBuilder builder = new StringBuilder();
          builder.append(method);
          builder.append(" ");
          builder.append(path);
          builder.append('\n');

          Enumeration<String> headerNames = request.getHeaderNames();
          while (headerNames.hasMoreElements())
          {
            String headerName = headerNames.nextElement();
            Enumeration<String> headers = request.getHeaders(headerName);
            while (headers.hasMoreElements())
            {
              String header = headers.nextElement();
              builder.append("   ");
              builder.append(headerName);
              builder.append(": ");
              builder.append(header);
              builder.append('\n');
            }
          }

          System.out.print(builder);
          System.out.flush();
        }

        if (path != null && method != null)
        {
          handle(method, path, request, response);

          if (DEBUG)
          {
            int status = response.getStatus();
            String reasonPhrase = getReasonPhrase(status);

            StringBuilder builder = new StringBuilder();
            builder.append(request.getProtocol());
            builder.append(" ");
            builder.append(status);
            if (!StringUtil.isEmpty(reasonPhrase))
            {
              builder.append(" ");
              builder.append(reasonPhrase);
            }

            builder.append('\n');

            for (String headerName : response.getHeaderNames())
            {
              for (String header : response.getHeaders(headerName))
              {
                builder.append("   ");
                builder.append(headerName);
                builder.append(": ");
                builder.append(header);
                builder.append('\n');
              }
            }

            System.out.println(builder);
          }
        }
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
        throw ex;
      }
      catch (RuntimeException ex)
      {
        ex.printStackTrace();
        throw ex;
      }
    }

    private void handle(String method, String path, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
      if (path.equals("/api/user/login") && HttpMethod.POST.is(method))
      {
        login(request, response);
        return;
      }

      Session session = getSession(request);
      if (session == null)
      {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }

      if (path.startsWith("/api/blob"))
      {
        User user = session.getUser();
        Path segments = new Path(path);

        String applicationToken = segments.segment(2);
        if (!applicationTokens.contains(applicationToken))
        {
          response.sendError(HttpServletResponse.SC_NOT_FOUND);
          return;
        }

        if (segments.segmentCount() < 4)
        {
          if (HttpMethod.GET.is(method))
          {
            File applicationFolder = getApplicationFolder(user, applicationToken);
            retrieveProperties(request, response, applicationFolder);
            return;
          }

          response.sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        }

        String key = segments.segment(3);
        File blobFile = getUserFile(user, applicationToken, key, BLOB_EXTENSION);
        File etagFile = getUserFile(user, applicationToken, key, ETAG_EXTENSION);
        boolean exists = etagFile.exists();

        if (HttpMethod.GET.is(method))
        {
          retrieveBlob(request, response, blobFile, etagFile, exists);
          return;
        }

        if (HttpMethod.PUT.is(method))
        {
          updateBlob(request, response, blobFile, etagFile, exists);
          return;
        }

        if (HttpMethod.DELETE.is(method))
        {
          deleteBlob(request, response, blobFile, etagFile, exists);
          return;
        }

        return;
      }

      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }

  /**
  * @author Eike Stepper
  */
  public static final class User
  {
    private final String username;

    private final byte[] password;

    public User(String username, String password)
    {
      this.username = username;
      this.password = StringUtil.encrypt(password);
    }

    public String getUsername()
    {
      return username;
    }

    public String getPassword()
    {
      return StringUtil.decrypt(password);
    }

    @Override
    public String toString()
    {
      return username;
    }

    @Override
    public int hashCode()
    {
      return username.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }

      if (obj == null)
      {
        return false;
      }

      if (getClass() != obj.getClass())
      {
        return false;
      }

      User other = (User)obj;
      if (username == null)
      {
        if (other.username != null)
        {
          return false;
        }
      }
      else if (!username.equals(other.username))
      {
        return false;
      }

      return true;
    }
  }

  /**
  * @author Eike Stepper
  */
  public static final class Session
  {
    private final String id;

    private final String csrfToken;

    private final User user;

    public Session(User user)
    {
      id = UUID.randomUUID().toString();
      csrfToken = UUID.randomUUID().toString();
      this.user = user;
    }

    public String getID()
    {
      return id;
    }

    public String getCSRFToken()
    {
      return csrfToken;
    }

    public User getUser()
    {
      return user;
    }

    @Override
    public int hashCode()
    {
      return id.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }

      if (obj == null)
      {
        return false;
      }

      if (getClass() != obj.getClass())
      {
        return false;
      }

      Session other = (Session)obj;
      if (id == null)
      {
        if (other.id != null)
        {
          return false;
        }
      }
      else if (!id.equals(other.id))
      {
        return false;
      }

      return true;
    }

    @Override
    public String toString()
    {
      return id + " -> " + user;
    }
  }

  /**
   * @author Eike Stepper
   */
  public static class NOOPLogger implements Logger
  {
    @Override
    public String getName()
    {
      return "noop";
    }

    @Override
    public void warn(String msg, Object... args)
    {
    }

    @Override
    public void warn(Throwable thrown)
    {
    }

    @Override
    public void warn(String msg, Throwable thrown)
    {
    }

    @Override
    public void info(String msg, Object... args)
    {
    }

    @Override
    public void info(Throwable thrown)
    {
    }

    @Override
    public void info(String msg, Throwable thrown)
    {
    }

    @Override
    public boolean isDebugEnabled()
    {
      return false;
    }

    @Override
    public void setDebugEnabled(boolean enabled)
    {
    }

    @Override
    public void debug(String msg, Object... args)
    {
    }

    @Override
    public void debug(Throwable thrown)
    {
    }

    @Override
    public void debug(String msg, Throwable thrown)
    {
    }

    @Override
    public void debug(String msg, long value)
    {
    }

    @Override
    public Logger getLogger(String name)
    {
      return this;
    }

    @Override
    public void ignore(Throwable ignored)
    {
    }
  }

  public static void main(String[] args) throws Exception
  {
    Log.setLog(new NOOPLogger());

    USSServer server = new USSServer(8080, new File(System.getProperty("java.io.tmpdir"), "uss-server"));
    server.addUser(FixedCredentialsProvider.DEFAULT_CREDENTIALS);

    Set<String> applicationTokens = server.getApplicationTokens();
    applicationTokens.add(StorageTests.APPLICATION_TOKEN);
    applicationTokens.add("cNhDr0INs8T109P8h6E1r_GvU3I"); // Oomph

    System.out.println(server.getFolder());
    System.out.println("Listening on port " + server.start());
    server.join();
  }
}
