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
import org.eclipse.userstorage.internal.StorageService.DynamicStorage;
import org.eclipse.userstorage.internal.util.StringUtil;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryEventListener;
import org.eclipse.core.runtime.Platform;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Eike Stepper
 */
public final class StorageServiceRegistry implements IStorageService.Registry
{
  public static final StorageServiceRegistry INSTANCE = new StorageServiceRegistry();

  private static final ExtensionPointHandler HANDLER = new ExtensionPointHandler();

  private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

  private final Map<URI, IStorageService> storages = new LinkedHashMap<URI, IStorageService>();

  private final LinkedList<IStorageService> defaultStorages = new LinkedList<IStorageService>();

  private boolean running;

  private StorageServiceRegistry()
  {
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

  @Override
  public IStorageService[] getServices()
  {
    synchronized (storages)
    {
      start();
      return storages.values().toArray(new IStorageService[storages.size()]);
    }
  }

  @Override
  public IStorageService getService(URI serviceURI)
  {
    synchronized (storages)
    {
      start();
      return storages.get(serviceURI);
    }
  }

  @Override
  public IStorageService.Dynamic addService(String serviceLabel, URI serviceURI, URI createAccountURI, URI editAccountURI, URI recoverPasswordURI)
      throws IllegalStateException
  {
    DynamicStorage storage = new DynamicStorage(serviceLabel, serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
    addStorage(storage);
    return storage;
  }

  @Override
  public IStorageService.Dynamic addService(String serviceLabel, URI serviceURI) throws IllegalStateException
  {
    return addService(serviceLabel, serviceURI, null, null, null);
  }

  void addStorage(IStorageService storage) throws IllegalStateException
  {
    URI serviceURI = storage.getServiceURI();

    synchronized (storages)
    {
      start();

      IStorageService registered = storages.get(serviceURI);
      if (registered != null)
      {
        throw new IllegalStateException("Storage already registered: " + registered);
      }

      storages.put(serviceURI, storage);
    }

    for (Listener listener : listeners)
    {
      try
      {
        listener.serviceAdded(storage);
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }
  }

  void removeStorage(IStorageService storage)
  {
    URI serviceURI = storage.getServiceURI();

    synchronized (storages)
    {
      start();
      storages.remove(serviceURI);
    }

    for (Listener listener : listeners)
    {
      try
      {
        listener.serviceRemoved(storage);
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }
  }

  void start()
  {
    synchronized (storages)
    {
      if (!running)
      {
        try
        {
          URI serviceURI = StringUtil.newURI("https://api-staging.eclipse.org/");
          URI createAccountURI = StringUtil.newURI("https://dev.eclipse.org/site_login/");
          URI editAccountURI = StringUtil.newURI("https://dev.eclipse.org/site_login/myaccount.php");
          URI recoverPasswordURI = StringUtil.newURI("https://dev.eclipse.org/site_login/password_recovery.php");

          StorageService eclipseStorage = new StorageService("Eclipse.org", serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
          storages.put(serviceURI, eclipseStorage);

          if (Activator.PLATFORM_RUNNING)
          {
            HANDLER.start();
          }
        }
        catch (Exception ex)
        {
          Activator.log(ex);
        }
        finally
        {
          running = true;
        }
      }
    }
  }

  void stop() throws Exception
  {
    synchronized (storages)
    {
      if (running)
      {
        try
        {
          if (Activator.PLATFORM_RUNNING)
          {
            HANDLER.stop();
          }

          storages.clear();
          defaultStorages.clear();
        }
        catch (Exception ex)
        {
          Activator.log(ex);
        }
        finally
        {
          running = false;
        }
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  private static final class ExtensionPointHandler implements IRegistryEventListener
  {
    private static final String EXTENSION_POINT = Activator.PLUGIN_ID + ".storages";

    public ExtensionPointHandler()
    {
    }

    public void start() throws Exception
    {
      IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
      for (IConfigurationElement configurationElement : extensionRegistry.getConfigurationElementsFor(EXTENSION_POINT))
      {
        added(configurationElement);
      }

      extensionRegistry.addListener(this, EXTENSION_POINT);
    }

    public void stop() throws Exception
    {
      Platform.getExtensionRegistry().removeListener(this);
    }

    @Override
    public void added(IExtensionPoint[] extensionPoints)
    {
      // Do nothing
    }

    @Override
    public void removed(IExtensionPoint[] extensionPoints)
    {
      // Do nothing
    }

    @Override
    public void added(IExtension[] extensions)
    {
      for (IExtension extension : extensions)
      {
        for (IConfigurationElement configurationElement : extension.getConfigurationElements())
        {
          added(configurationElement);
        }
      }
    }

    @Override
    public void removed(IExtension[] extensions)
    {
      for (IExtension extension : extensions)
      {
        for (IConfigurationElement configurationElement : extension.getConfigurationElements())
        {
          removed(configurationElement);
        }
      }
    }

    private void added(IConfigurationElement configurationElement)
    {
      try
      {
        StorageService storage = createStorage(configurationElement);
        INSTANCE.addStorage(storage);
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }

    private void removed(IConfigurationElement configurationElement)
    {
      try
      {
        StorageService storage = createStorage(configurationElement);
        INSTANCE.removeStorage(storage);
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }

    private StorageService createStorage(IConfigurationElement configurationElement)
    {
      String serviceLabel = configurationElement.getAttribute("serviceLabel");
      URI serviceURI = StringUtil.newURI(configurationElement.getAttribute("serviceURI"));
      URI createAccountURI = StringUtil.newURI(configurationElement.getAttribute("createAccountURI"));
      URI editAccountURI = StringUtil.newURI(configurationElement.getAttribute("editAccountURI"));
      URI recoverPasswordURI = StringUtil.newURI(configurationElement.getAttribute("recoverPasswordURI"));

      return new StorageService(serviceLabel, serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
    }
  }
}
