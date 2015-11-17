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

import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.internal.util.IOUtil.EndOfFileAware;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public abstract class InternalStorageCache
{
  private IStorageService service;

  public InternalStorageCache()
  {
  }

  public IStorageService getService()
  {
    return service;
  }

  void setService(IStorageService service)
  {
    IStorageService oldService = this.service;
    this.service = service;

    if (service != oldService)
    {
      storageChanged(oldService, service);
    }
  }

  protected abstract void storageChanged(IStorageService oldService, IStorageService newService);

  void internalLoadProperties(String applicationToken, String key, Map<String, String> properties) throws IOException
  {
    loadProperties(applicationToken, key, properties);
  }

  void internalSaveProperties(String applicationToken, String key, Map<String, String> properties) throws IOException
  {
    saveProperties(applicationToken, key, properties);
  }

  InputStream internalGetInputStream(String applicationToken, String key) throws IOException
  {
    return getInputStream(applicationToken, key);
  }

  OutputStream internalGetOutputStream(final String applicationToken, final String key, final Map<String, String> properties) throws IOException
  {
    return new TransactionalOutputStream(applicationToken, key, properties);
  }

  void internalDelete(String applicationToken, String key) throws IOException
  {
    delete(applicationToken, key);
  }

  public abstract Iterator<String> getKeys(String applicationToken) throws IOException;

  protected abstract void loadProperties(String applicationToken, String key, Map<String, String> properties) throws IOException;

  protected abstract void saveProperties(String applicationToken, String key, Map<String, String> properties) throws IOException;

  protected abstract InputStream getInputStream(String applicationToken, String key) throws IOException;

  protected abstract OutputStream getOutputStream(String applicationToken, String key) throws IOException;

  protected abstract void delete(String applicationToken, String key) throws IOException;

  /**
   * @author Eike Stepper
   */
  private final class TransactionalOutputStream extends OutputStream implements EndOfFileAware
  {
    private final String applicationToken;

    private final String key;

    private final Map<String, String> properties;

    private OutputStream output;

    private boolean fullyWritten;

    private TransactionalOutputStream(String applicationToken, String key, Map<String, String> properties)
    {
      this.applicationToken = applicationToken;
      this.key = key;
      this.properties = properties;
    }

    private void init() throws IOException
    {
      if (output == null)
      {
        saveProperties(applicationToken, key, Blob.NO_PROPERTIES);
        output = getOutputStream(applicationToken, key);
      }
    }

    @Override
    public void write(int b) throws IOException
    {
      init();
      output.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException
    {
      init();
      output.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
      init();
      output.write(b, off, len);
    }

    @Override
    public void reachedEndOfFile()
    {
      fullyWritten = true;
    }

    @Override
    public void close() throws IOException
    {
      if (output != null)
      {
        try
        {
          if (fullyWritten)
          {
            saveProperties(applicationToken, key, properties);
          }
        }
        finally
        {
          output.close();
          output = null;
        }
      }
    }
  }
}
