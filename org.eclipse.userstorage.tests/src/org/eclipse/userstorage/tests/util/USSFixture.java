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

import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.StorageFactory;
import org.eclipse.userstorage.internal.Activator;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.spi.ISettings;
import org.eclipse.userstorage.tests.util.USSServer.NOOPLogger;
import org.eclipse.userstorage.tests.util.USSServer.User;
import org.eclipse.userstorage.util.FileStorageCache;
import org.eclipse.userstorage.util.Settings.MemorySettings;

import org.eclipse.jetty.util.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * @author Eike Stepper
 */
public class USSFixture
{
  private static final boolean REMOTE = Boolean.getBoolean("org.eclipse.userstorage.tests.remote");

  private static final File TEST_FOLDER = new File(System.getProperty("java.io.tmpdir"), "uss-tests");

  private static final File SERVER_FOLDER = new File(TEST_FOLDER, "server");

  private static final File CACHE = new File(TEST_FOLDER, "cache");

  private final String applicationToken;

  private USSServer server;

  private User user;

  private IStorageService.Dynamic service;

  private StorageFactory factory;

  private TestCache cache;

  public USSFixture(String applicationToken) throws Exception
  {
    this.applicationToken = applicationToken;

    configureServerLogging();
    if (manageBundleLifecycle())
    {
      Activator.start();
    }

    if (REMOTE)
    {
      service = IStorageService.Registry.INSTANCE.addService("Eclipse.org (Staging)", StringUtil.newURI("https://api-staging.eclipse.org/"));
    }
    else
    {
      IOUtil.deleteFiles(SERVER_FOLDER);

      server = new USSServer(8080, SERVER_FOLDER);
      user = server.addUser(FixedCredentialsProvider.DEFAULT_CREDENTIALS);
      server.getApplicationTokens().add(applicationToken);
      int port = server.start();

      service = IStorageService.Registry.INSTANCE.addService("Local", StringUtil.newURI("http://localhost:" + port));
    }

    ISettings settings = new MemorySettings(Collections.singletonMap(applicationToken, service.getServiceURI().toString()));
    factory = new StorageFactory(settings);

    IOUtil.deleteFiles(CACHE);
    cache = new TestCache(CACHE);
  }

  public final String getApplicationToken()
  {
    return applicationToken;
  }

  public final boolean hasLocalServer()
  {
    return server != null;
  }

  public final USSServer getServer()
  {
    return server;
  }

  public final User getUser()
  {
    return user;
  }

  public final IStorageService.Dynamic getService()
  {
    return service;
  }

  public final StorageFactory getFactory()
  {
    return factory;
  }

  public final TestCache getCache()
  {
    return cache;
  }

  public final String readCache(String key, String extension) throws IOException
  {
    try
    {
      return IOUtil.readUTF(cache.getFile(applicationToken, key, extension));
    }
    catch (RuntimeException ex)
    {
      Throwable cause = ex.getCause();
      if (cause instanceof IOException)
      {
        throw (IOException)cause;
      }

      throw ex;
    }
  }

  public final BlobInfo readServer(IBlob blob) throws IOException
  {
    BlobInfo result = new BlobInfo();

    String applicationToken = blob.getStorage().getApplicationToken();
    String key = blob.getKey();

    if (hasLocalServer())
    {
      try
      {
        File blobFile = server.getUserFile(user, applicationToken, key, null);
        File etagFile = server.getUserFile(user, applicationToken, key, ".etag");
        if (!blobFile.isFile() || !etagFile.isFile())
        {
          return null;
        }

        result.contents = IOUtil.readUTF(blobFile);
        result.eTag = IOUtil.readUTF(etagFile);
      }
      catch (RuntimeException ex)
      {
        Throwable cause = ex.getCause();
        if (cause instanceof IOException)
        {
          throw (IOException)cause;
        }

        throw ex;
      }
    }
    else
    {
      IStorage tmpStorage = factory.create(applicationToken);
      IBlob tmpBlob = tmpStorage.getBlob(key);

      result.contents = tmpBlob.getContentsUTF();
      if (result.contents == null)
      {
        return null;
      }

      result.eTag = tmpBlob.getETag();
    }

    return result;
  }

  public final String writeServer(IBlob blob, String value) throws IOException
  {
    String applicationToken = blob.getStorage().getApplicationToken();
    String key = blob.getKey();

    if (hasLocalServer())
    {
      String eTag = UUID.randomUUID().toString();
      IOUtil.writeUTF(server.getUserFile(user, applicationToken, key, ".etag"), eTag);
      IOUtil.writeUTF(server.getUserFile(user, applicationToken, key, null), value);
      return eTag;
    }

    IStorage tmpStorage = factory.create(applicationToken);
    IBlob tmpBlob = tmpStorage.getBlob(key);
    tmpBlob.setETag(blob.getETag());
    tmpBlob.setContentsUTF(value);
    return tmpBlob.getETag();
  }

  public void dispose() throws Exception
  {
    factory = null;
    cache = null;
    user = null;

    if (service != null)
    {
      service.remove();
      service = null;
    }

    if (server != null)
    {
      server.stop();
      server = null;
    }

    if (manageBundleLifecycle())
    {
      Activator.stop();
    }
  }

  protected void configureServerLogging()
  {
    Log.setLog(new NOOPLogger());
  }

  protected boolean manageBundleLifecycle()
  {
    return true;
  }

  /**
   * @author Eike Stepper
   */
  public static final class BlobInfo
  {
    public String eTag;

    public String contents;
  }

  /**
   * @author Eike Stepper
   */
  public static final class TestCache extends FileStorageCache
  {
    public TestCache(File folder)
    {
      super(folder);
    }

    @Override
    public File getFile(String applicationToken, String key, String extension)
    {
      return super.getFile(applicationToken, key, extension);
    }
  }
}
