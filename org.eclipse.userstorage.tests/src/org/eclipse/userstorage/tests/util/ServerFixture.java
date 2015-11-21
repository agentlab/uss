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
import org.eclipse.userstorage.util.Settings.MemorySettings;

import org.eclipse.jetty.util.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Eike Stepper
 */
public class ServerFixture extends Fixture
{
  public static final File SERVER_FOLDER = new File(TEST_FOLDER, "server");

  private final String applicationToken;

  private USSServer server;

  private User user;

  private IStorageService.Dynamic service;

  public ServerFixture(String applicationToken) throws Exception
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

  public final StorageFactory createFactory(String applicationToken) throws Exception
  {
    ISettings settings = new MemorySettings();
    settings.setValue(applicationToken, service.getServiceURI().toString());

    return new StorageFactory(settings);
  }

  public final BlobInfo readServer(IBlob blob) throws Exception
  {
    BlobInfo result = new BlobInfo();

    String applicationToken = blob.getStorage().getApplicationToken();
    String key = blob.getKey();

    if (hasLocalServer())
    {
      try
      {
        File blobFile = server.getUserFile(user, applicationToken, key, USSServer.BLOB_EXTENSION);
        File etagFile = server.getUserFile(user, applicationToken, key, USSServer.ETAG_EXTENSION);
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
      StorageFactory factory = createFactory(applicationToken);
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

  public final String writeServer(IBlob blob, String value) throws Exception
  {
    String applicationToken = blob.getStorage().getApplicationToken();
    String key = blob.getKey();

    if (hasLocalServer())
    {
      String eTag = UUID.randomUUID().toString();
      IOUtil.writeUTF(server.getUserFile(user, applicationToken, key, USSServer.ETAG_EXTENSION), eTag);
      IOUtil.writeUTF(server.getUserFile(user, applicationToken, key, USSServer.BLOB_EXTENSION), value);
      return eTag;
    }

    StorageFactory factory = createFactory(applicationToken);
    IStorage tmpStorage = factory.create(applicationToken);
    IBlob tmpBlob = tmpStorage.getBlob(key);
    tmpBlob.setETag(blob.getETag());
    tmpBlob.setContentsUTF(value);
    return tmpBlob.getETag();
  }

  @Override
  public void dispose() throws Exception
  {
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

    super.dispose();
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
}
