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
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.IStorageService.Registry;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.spi.StorageCache;
import org.eclipse.userstorage.util.BadApplicationTokenException;

import java.util.NoSuchElementException;

/**
 * @author Eike Stepper
 */
public abstract class InternalStorageFactory
{
  private static final Registry REGISTRY = IStorageService.Registry.INSTANCE;

  public InternalStorageFactory()
  {
  }

  public IStorage create(String applicationToken, StorageCache cache) throws NoSuchElementException, BadApplicationTokenException
  {
    IStorageService service = getService(applicationToken);

    Storage storage = new Storage(this, applicationToken, cache);
    storage.setService(service);
    return storage;
  }

  private IStorageService getService(String applicationToken) throws NoSuchElementException
  {
    try
    {
      String serviceURI = getPreferredServiceURI(applicationToken);
      if (serviceURI != null)
      {
        IStorageService service = REGISTRY.getService(StringUtil.newURI(serviceURI));
        if (service != null)
        {
          return service;
        }
      }
    }
    catch (Exception ex)
    {
      //$FALL-THROUGH$
    }

    try
    {
      String serviceURI = getPreferredServiceURI(null);
      if (serviceURI != null)
      {
        IStorageService service = REGISTRY.getService(StringUtil.newURI(serviceURI));
        if (service != null)
        {
          return service;
        }
      }
    }
    catch (Exception ex)
    {
      //$FALL-THROUGH$
    }

    IStorageService[] storages = REGISTRY.getServices();
    if (storages.length != 0)
    {
      return storages[0];
    }

    throw new NoSuchElementException("No service registered");
  }

  protected abstract String getPreferredServiceURI(String applicationToken);

  protected abstract void setPreferredServiceURI(String applicationToken, String serviceURI);
}
