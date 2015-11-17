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
import org.eclipse.userstorage.StorageFactory;
import org.eclipse.userstorage.internal.util.IOUtil.TeeInputStream;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.spi.ISettings;
import org.eclipse.userstorage.spi.StorageCache;
import org.eclipse.userstorage.util.BadApplicationTokenException;
import org.eclipse.userstorage.util.ConflictException;
import org.eclipse.userstorage.util.NoServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Eike Stepper
 */
public final class Storage implements IStorage
{
  private static final String DEFAULT_APPLICATION_TOKEN = "<default>";

  private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

  private final String applicationToken;

  private final StorageFactory factory;

  private final InternalStorageCache cache;

  private final Map<String, Blob> blobs = new WeakHashMap<String, Blob>();

  private StorageService service;

  public Storage(StorageFactory factory, String applicationToken, InternalStorageCache cache) throws BadApplicationTokenException
  {
    this.applicationToken = BadApplicationTokenException.validate(applicationToken);
    this.factory = factory;
    this.cache = cache;

    StorageServiceRegistry.INSTANCE.addStorage(this);
  }

  @Override
  public String getApplicationToken()
  {
    return applicationToken;
  }

  @Override
  public StorageFactory getFactory()
  {
    return factory;
  }

  @Override
  public StorageCache getCache()
  {
    return (StorageCache)cache;
  }

  @Override
  public StorageService getService()
  {
    StorageService oldService;
    StorageService newService;

    synchronized (this)
    {
      oldService = service;
      newService = oldService;

      if (newService != null)
      {
        URI serviceURI = newService.getServiceURI();
        service = newService = StorageServiceRegistry.INSTANCE.getService(serviceURI);
      }

      if (newService == null)
      {
        service = newService = lookupService();
        if (cache != null)
        {
          cache.setService(newService);
        }
      }
    }

    notifyListeners(oldService, newService);
    return newService;
  }

  @Override
  public void setService(IStorageService service)
  {
    StorageService oldService;

    synchronized (this)
    {
      oldService = this.service;

      if (service != oldService)
      {
        disposeBlobs();

        this.service = (StorageService)service;

        String serviceURI = service == null ? null : service.getServiceURI().toString();
        setServiceURI(serviceURI);

        if (cache != null)
        {
          cache.setService(service);
        }
      }
    }

    notifyListeners(oldService, service);
  }

  @Override
  public IBlob getBlob(String key)
  {
    synchronized (this)
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
  }

  @Override
  public void addListener(Listener listener)
  {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener)
  {
    listeners.remove(listener);
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

  public InputStream retrieveBlob(String key, Map<String, String> properties) throws IOException, NoServiceException
  {
    StorageService service = getServiceSafe();
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

  public boolean updateBlob(String key, Map<String, String> properties, InputStream in) throws IOException, ConflictException, NoServiceException
  {
    StorageService service = getServiceSafe();

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

  void serviceRemoved(IStorageService service)
  {
    if (this.service == service)
    {
      setService(null);
    }
  }

  private StorageService getServiceSafe()
  {
    StorageService service = getService();
    if (service != null)
    {
      return service;
    }

    throw new NoServiceException();
  }

  private StorageService lookupService()
  {
    if (!StringUtil.isEmpty(applicationToken))
    {
      StorageService service = lookupService(applicationToken);
      if (service != null)
      {
        return service;
      }
    }

    StorageService service = lookupService(DEFAULT_APPLICATION_TOKEN);
    if (service != null)
    {
      return service;
    }

    return StorageServiceRegistry.INSTANCE.getFirstService();
  }

  private StorageService lookupService(String applicationToken)
  {
    try
    {
      String serviceURI = getServiceURI(applicationToken);
      if (serviceURI != null)
      {
        return StorageServiceRegistry.INSTANCE.getService(StringUtil.newURI(serviceURI));
      }
    }
    catch (Exception ex)
    {
      //$FALL-THROUGH$
    }

    return null;
  }

  private String getServiceURI(String applicationToken)
  {
    try
    {
      ISettings settings = factory.getSettings();
      return settings.getValue(applicationToken);
    }
    catch (Exception ex)
    {
      Activator.log(ex);
    }

    return null;
  }

  private void setServiceURI(String serviceURI)
  {
    try
    {
      ISettings settings = factory.getSettings();
      settings.setValue(applicationToken, serviceURI);
    }
    catch (Exception ex)
    {
      Activator.log(ex);
    }
  }

  private void notifyListeners(IStorageService oldService, IStorageService newService)
  {
    if (oldService != newService)
    {
      for (Listener listener : listeners)
      {
        try
        {
          listener.serviceChanged(this, oldService, newService);
        }
        catch (Exception ex)
        {
          Activator.log(ex);
        }
      }
    }
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
