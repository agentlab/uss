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
import org.eclipse.userstorage.internal.Storage.DynamicStorage;
import org.eclipse.userstorage.internal.util.StringUtil;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryEventListener;
import org.eclipse.core.runtime.Platform;

import org.osgi.service.prefs.Preferences;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author Eike Stepper
 */
public final class StorageRegistry implements IStorage.Registry
{
  public static final StorageRegistry INSTANCE = new StorageRegistry();

  private static final boolean TRANSIENT_APPLICATIONS = Boolean.getBoolean(StorageRegistry.class.getName() + ".transientApplications");

  private static final ExtensionPointHandler HANDLER = new ExtensionPointHandler();

  private final Map<URI, IStorage> storages = new LinkedHashMap<URI, IStorage>();

  private final LinkedList<IStorage> defaultStorages = new LinkedList<IStorage>();

  private StorageRegistry()
  {
  }

  @Override
  public IStorage[] getStorages()
  {
    synchronized (storages)
    {
      return storages.values().toArray(new IStorage[storages.size()]);
    }
  }

  @Override
  public IStorage getStorage(URI serviceURI)
  {
    synchronized (storages)
    {
      return storages.get(serviceURI);
    }
  }

  public IStorage getStorage(String applicationToken) throws NoSuchElementException
  {
    if (Activator.PLATFORM_RUNNING && !TRANSIENT_APPLICATIONS)
    {
      try
      {
        Preferences spaces = getPreferences();

        String serviceURI = spaces.get(applicationToken, null);
        if (serviceURI != null)
        {
          IStorage storage = getStorage(StringUtil.newURI(serviceURI));
          if (storage != null)
          {
            return storage;
          }
        }
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }

    return getDefaultStorage();
  }

  public void setStorage(String applicationToken, IStorage storage)
  {
    if (Activator.PLATFORM_RUNNING && !TRANSIENT_APPLICATIONS)
    {
      try
      {
        Preferences spaces = getPreferences();
        spaces.put(applicationToken, storage.getServiceURI().toString());
        spaces.flush();
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }
  }

  @Override
  public IStorage getDefaultStorage() throws NoSuchElementException
  {
    synchronized (storages)
    {
      if (!defaultStorages.isEmpty())
      {
        return defaultStorages.getLast();
      }

      if (!storages.isEmpty())
      {
        return storages.values().iterator().next();
      }

      throw new NoSuchElementException("No storage exists");
    }
  }

  @Override
  public void setDefaultStorage(IStorage defaultStorage)
  {
    // TODO Add a check to ensure that a passed dynamic storage is currently registered.
    if (defaultStorage != null)
    {
      synchronized (storages)
      {
        if (defaultStorages.isEmpty() || defaultStorages.getLast() != defaultStorage)
        {
          defaultStorages.add(defaultStorage);
        }
      }
    }
  }

  @Override
  public IStorage.Dynamic addStorage(String serviceLabel, URI serviceURI, URI createAccountURI, URI editAccountURI, URI recoverPasswordURI)
      throws IllegalStateException
  {
    DynamicStorage storage = new DynamicStorage(serviceLabel, serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
    addStorage(storage);
    return storage;
  }

  @Override
  public IStorage.Dynamic addStorage(String serviceLabel, URI serviceURI) throws IllegalStateException
  {
    return addStorage(serviceLabel, serviceURI, null, null, null);
  }

  void addStorage(IStorage storage) throws IllegalStateException
  {
    URI serviceURI = storage.getServiceURI();

    synchronized (storages)
    {
      IStorage registered = storages.get(serviceURI);
      if (registered != null)
      {
        throw new IllegalStateException("Storage already registered: " + registered);
      }

      storages.put(serviceURI, storage);
    }
  }

  void removeStorage(IStorage storage)
  {
    URI serviceURI = storage.getServiceURI();

    synchronized (storages)
    {
      storages.remove(serviceURI);

      IStorage lastDefaultStorage = null;
      for (Iterator<IStorage> it = defaultStorages.iterator(); it.hasNext();)
      {
        IStorage defaultStorage = it.next();
        if (defaultStorage == storage)
        {
          it.remove();
        }
        else
        {
          if (lastDefaultStorage == null)
          {
            lastDefaultStorage = defaultStorage;
          }
          else
          {
            if (lastDefaultStorage == defaultStorage)
            {
              it.remove();
            }
            else
            {
              lastDefaultStorage = defaultStorage;
            }
          }
        }
      }
    }
  }

  void start() throws Exception
  {
    synchronized (storages)
    {
      URI serviceURI = StringUtil.newURI("https://api-staging.eclipse.org/");
      URI createAccountURI = StringUtil.newURI("https://dev.eclipse.org/site_login/");
      URI editAccountURI = StringUtil.newURI("https://dev.eclipse.org/site_login/myaccount.php");
      URI recoverPasswordURI = StringUtil.newURI("https://dev.eclipse.org/site_login/password_recovery.php");

      Storage eclipseStorage = new Storage("Eclipse.org", serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
      storages.put(serviceURI, eclipseStorage);

      if (Activator.PLATFORM_RUNNING)
      {
        HANDLER.start();
      }
    }
  }

  void stop() throws Exception
  {
    synchronized (storages)
    {
      if (Activator.PLATFORM_RUNNING)
      {
        HANDLER.stop();
      }

      storages.clear();
      defaultStorages.clear();
    }
  }

  private static Preferences getPreferences()
  {
    return Platform.getPreferencesService().getRootNode().node("configuration").node(Activator.PLUGIN_ID).node("spaces");
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
        Storage storage = createStorage(configurationElement);
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
        Storage storage = createStorage(configurationElement);
        INSTANCE.removeStorage(storage);
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }

    private Storage createStorage(IConfigurationElement configurationElement)
    {
      String serviceLabel = configurationElement.getAttribute("serviceLabel");
      URI serviceURI = StringUtil.newURI(configurationElement.getAttribute("serviceURI"));
      URI createAccountURI = StringUtil.newURI(configurationElement.getAttribute("createAccountURI"));
      URI editAccountURI = StringUtil.newURI(configurationElement.getAttribute("editAccountURI"));
      URI recoverPasswordURI = StringUtil.newURI(configurationElement.getAttribute("recoverPasswordURI"));

      return new Storage(serviceLabel, serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
    }
  }
}
