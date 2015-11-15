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
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.internal.util.IOUtil.TeeInputStream;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.spi.StorageCache;
import org.eclipse.userstorage.util.BadApplicationTokenException;
import org.eclipse.userstorage.util.ConflictException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Eike Stepper
 */
public final class Storage implements IStorage
{
  private final String applicationToken;

  private final InternalStorageFactory factory;

  private final InternalStorageCache cache;

  private final Map<String, Blob> blobs = new WeakHashMap<String, Blob>();

  private StorageService service;

  public Storage(InternalStorageFactory factory, String applicationToken, InternalStorageCache cache) throws BadApplicationTokenException
  {
    this.applicationToken = BadApplicationTokenException.validate(applicationToken);
    this.factory = factory;
    this.cache = cache;
  }

  @Override
  public String getApplicationToken()
  {
    return applicationToken;
  }

  @Override
  public StorageCache getCache()
  {
    return (StorageCache)cache;
  }

  @Override
  public StorageService getService()
  {
    return service;
  }

  @Override
  public synchronized void setService(IStorageService service)
  {
    if (service != this.service)
    {
      disposeBlobs();

      this.service = (StorageService)service;
      factory.setPreferredServiceURI(applicationToken, service.getServiceURI().toString());

      if (cache != null)
      {
        // TODO Should this happen later when the new storage is accessed the first time?
        cache.setService(service);
      }
    }
  }

  @Override
  public synchronized IBlob getBlob(String key)
  {
    Blob blob = blobs.get(key);
    if (blob == null)
    {
      Map<String, String> properties = new HashMap<String, String>();

      if (cache != null)
      {
        try
        {
          cache.internalLoadProperties(applicationToken, key, properties);
        }
        catch (IOException ex)
        {
          properties.clear();
          Activator.log(ex);
        }
      }

      blob = new Blob(this, key, properties);
      blobs.put(key, blob);
    }

    return blob;
  }

  public void setETag(String key, Map<String, String> properties, String eTag)
  {
    if (StringUtil.isEmpty(eTag))
    {
      properties.remove(Blob.ETAG);
    }
    else
    {
      properties.put(Blob.ETAG, eTag);
    }

    if (cache != null)
    {
      try
      {
        if (!StringUtil.isEmpty(eTag))
        {
          Map<String, String> cacheProperties = new HashMap<String, String>();
          cache.loadProperties(applicationToken, key, cacheProperties);
          String cacheETag = cacheProperties.get(Blob.ETAG);
          if (eTag.equals(cacheETag))
          {
            // Don't delete blob from cache because it has the new ETag.
            return;
          }
        }

        cache.internalDelete(applicationToken, key);
      }
      catch (IOException ex)
      {
        Activator.log(ex);
      }
    }
  }

  public InputStream retrieveBlob(String key, Map<String, String> properties) throws IOException
  {
    InputStream contents = service.retrieveBlob(applicationToken, key, properties, cache != null);

    if (cache != null)
    {
      if (contents == Blob.NOT_MODIFIED)
      {
        return cache.internalGetInputStream(applicationToken, key);
      }

      if (contents == null)
      {
        cache.internalDelete(applicationToken, key);
        return null;
      }

      OutputStream output = cache.internalGetOutputStream(applicationToken, key, properties);
      return new TeeInputStream(contents, output);
    }

    return contents;
  }

  public boolean updateBlob(String key, Map<String, String> properties, InputStream in) throws IOException, ConflictException
  {
    if (cache != null)
    {
      OutputStream output = cache.internalGetOutputStream(applicationToken, key, properties);
      in = new TeeInputStream(in, output);
    }

    boolean created = service.updateBlob(applicationToken, key, properties, in);

    if (cache != null)
    {
      cache.internalSaveProperties(applicationToken, key, properties);
    }

    return created;
  }

  @Override
  public String toString()
  {
    return service + " (" + applicationToken + ")";
  }

  private void disposeBlobs()
  {
    for (Blob blob : blobs.values())
    {
      blob.dispose();
    }

    blobs.clear();

    if (cache != null)
    {
      try
      {
        for (Iterator<String> it = cache.getKeys(applicationToken); it.hasNext();)
        {
          String key = it.next();

          try
          {
            cache.delete(applicationToken, key);
          }
          catch (Exception ex)
          {
            Activator.log(ex);
          }
        }
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }
  }
}
