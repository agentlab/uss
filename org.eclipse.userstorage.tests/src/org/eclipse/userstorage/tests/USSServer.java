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
package org.eclipse.userstorage.tests;

import org.eclipse.userstorage.internal.Blob;
import org.eclipse.userstorage.internal.Credentials;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.JSONUtil;
import org.eclipse.userstorage.internal.util.StringUtil;

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
  private static final boolean DEBUG = Boolean.getBoolean(USSServer.class.getName() + ".debug");

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
    return new File(new File(new File(folder, user.getUsername()), applicationToken), key + StringUtil.safe(extension));
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
    Map<String, String> properties = new LinkedHashMap<String, String>();
    IOUtil.closeSilent(JSONUtil.decode(request.getInputStream(), properties, null));

    String username = properties.get("username");
    String password = properties.get("password");

    User user = users.get(username);
    if (password == null || !password.equals(user.getPassword()))
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

    properties.clear();
    properties.put("sessid", session.getID());
    properties.put("token", session.getCSRFToken());
    InputStream body = JSONUtil.encode(properties, null, null);

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
    String ifNonMatch = getETag(request, "If-Non-Match");
    if (ifNonMatch != null && ifNonMatch.equals(etag))
    {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");
    response.setHeader("ETag", "\"" + etag + "\"");

    InputStream body = JSONUtil.encode(Blob.NO_PROPERTIES, "value", new FileInputStream(blobFile));

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

    String etag = UUID.randomUUID().toString();

    IOUtil.mkdirs(blobFile.getParentFile());
    FileOutputStream out = new FileOutputStream(blobFile);
    InputStream body = null;

    try
    {
      Map<String, String> properties = new LinkedHashMap<String, String>();
      body = JSONUtil.decode(request.getInputStream(), properties, "value");
      IOUtil.copy(body, out);
    }
    finally
    {
      IOUtil.closeSilent(body);
      IOUtil.close(out);
    }

    IOUtil.writeUTF(etagFile, etag);

    response.setStatus(exists ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_CREATED);
    response.setHeader("ETag", "\"" + etag + "\"");
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

        String path = request.getPathInfo();
        String method = request.getMethod();

        if (DEBUG)
        {
          System.out.println(method + " " + path);
        }

        if (path != null && method != null)
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
            String key = segments.segment(3);

            if (!applicationTokens.contains(applicationToken))
            {
              response.sendError(HttpServletResponse.SC_NOT_FOUND);
              return;
            }

            File blobFile = getUserFile(user, applicationToken, key, null);
            File etagFile = getUserFile(user, applicationToken, key, ".etag");
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

            return;
          }

          response.sendError(HttpServletResponse.SC_FORBIDDEN);
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
  }

  /**
  * @author Eike Stepper
  */
  public static final class User
  {
    private final String username;

    private final String password;

    public User(String username, String password)
    {
      this.username = username;
      this.password = password;
    }

    public String getUsername()
    {
      return username;
    }

    public String getPassword()
    {
      return password;
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
    server.addUser("titusexperior@gmail.com", "abc92669183478");

    System.out.println("Listening on port " + server.start());
    server.join();
  }
}
