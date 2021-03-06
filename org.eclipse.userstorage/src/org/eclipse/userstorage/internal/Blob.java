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

import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.util.BadKeyException;
import org.eclipse.userstorage.util.ConflictException;
import org.eclipse.userstorage.util.NoServiceException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public class Blob implements IBlob
{
  public static final Map<String, String> NO_PROPERTIES = Collections.emptyMap();

  public static final InputStream NOT_MODIFIED = new InputStream()
  {
    @Override
    public int read() throws IOException
    {
      return -1;
    }
  };

  public static final String ETAG = "etag";

  private final Storage storage;

  private final String key;

  private final Map<String, String> properties;

  private boolean disposed;

  public Blob(Storage storage, String key, Map<String, String> properties) throws BadKeyException
  {
    this.storage = storage;
    this.key = BadKeyException.validate(key);
    this.properties = properties;
  }

  @Override
  public IStorage getStorage()
  {
    return storage;
  }

  @Override
  public String getKey()
  {
    return key;
  }

  @Override
  public Map<String, String> getProperties() throws IllegalStateException
  {
    checkNotDisposed();
    return Collections.unmodifiableMap(properties);
  }

  public void setProperties(Map<String, Object> properties)
  {
    for (Map.Entry<String, Object> entry : properties.entrySet())
    {
      String key = entry.getKey();
      Object value = entry.getValue();

      this.properties.put(key, String.valueOf(value));
    }
  }

  @Override
  public String getETag() throws IllegalStateException
  {
    checkNotDisposed();
    return properties.get(ETAG);
  }

  @Override
  public void setETag(String eTag) throws IllegalStateException
  {
    checkNotDisposed();
    storage.setETag(key, properties, eTag);
  }

  @Override
  public InputStream getContents() throws IOException, NoServiceException, IllegalStateException
  {
    checkNotDisposed();
    return storage.retrieveBlob(key, properties);
  }

  @Override
  public boolean setContents(InputStream in) throws IOException, ConflictException, NoServiceException, IllegalStateException
  {
    try
    {
      checkNotDisposed();
      return storage.updateBlob(key, properties, in);
    }
    finally
    {
      // Just a safety guard to ensure the stream is closed even if something goes wrong before it's even attempted to be read.
      IOUtil.closeSilent(in);
    }
  }

  @Override
  public String getContentsUTF() throws IOException, NoServiceException, IllegalStateException
  {
    InputStream contents = getContents();

    try
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      IOUtil.copy(contents, baos);
      return StringUtil.fromUTF(baos.toByteArray());
    }
    finally
    {
      IOUtil.close(contents);
    }
  }

  @Override
  public boolean setContentsUTF(String value) throws IOException, ConflictException, NoServiceException, IllegalStateException
  {
    return setContents(IOUtil.streamUTF(value));
  }

  @Override
  public int getContentsInt() throws IOException, NumberFormatException, NoServiceException, IllegalStateException
  {
    return Integer.parseInt(getContentsUTF());
  }

  @Override
  public boolean setContentsInt(int value) throws IOException, ConflictException, NoServiceException, IllegalStateException
  {
    return setContentsUTF(Integer.toString(value));
  }

  @Override
  public boolean getContentsBoolean() throws IOException, NoServiceException, IllegalStateException
  {
    return Boolean.parseBoolean(getContentsUTF());
  }

  @Override
  public boolean setContentsBoolean(boolean value) throws IOException, ConflictException, NoServiceException, IllegalStateException
  {
    return setContentsUTF(Boolean.toString(value));
  }

  @Override
  public boolean delete() throws IOException, ConflictException, NoServiceException, IllegalStateException
  {
    checkNotDisposed();
    return storage.deleteBlob(key, properties);
  }

  @Override
  public String toString()
  {
    return storage.getService() + " (" + storage.getApplicationToken() + "/" + key + ")";
  }

  @Override
  public boolean isDisposed()
  {
    return disposed;
  }

  void dispose()
  {
    disposed = true;
  }

  private void checkNotDisposed() throws IllegalStateException
  {
    if (disposed)
    {
      throw new IllegalStateException("Blob disposed: " + this);
    }
  }
}
