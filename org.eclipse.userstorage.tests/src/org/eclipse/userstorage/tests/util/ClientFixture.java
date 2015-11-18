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

import org.eclipse.userstorage.StorageFactory;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.spi.ISettings;
import org.eclipse.userstorage.util.FileStorageCache;
import org.eclipse.userstorage.util.Settings.MemorySettings;

import java.io.File;
import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class ClientFixture extends Fixture
{
  private static final File CACHE = new File(TEST_FOLDER, "cache");

  private final String applicationToken;

  private final StorageFactory factory;

  private final TestCache cache;

  public ClientFixture(ServerFixture serverFixture) throws Exception
  {
    applicationToken = serverFixture.getApplicationToken();
    String serviceURI = serverFixture.getService().getServiceURI().toString();

    ISettings settings = new MemorySettings();
    settings.setValue(applicationToken, serviceURI);

    factory = new StorageFactory(settings);
    cache = createCache();
  }

  public String getApplicationToken()
  {
    return applicationToken;
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

  protected TestCache createCache()
  {
    IOUtil.deleteFiles(CACHE);
    return new TestCache(CACHE);
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
