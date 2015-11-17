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
import org.eclipse.userstorage.internal.StorageService.DynamicService;
import org.eclipse.userstorage.internal.util.StringUtil;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryEventListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Eike Stepper
 */
public final class StorageServiceRegistry implements IStorageService.Registry
{
  public static final StorageServiceRegistry INSTANCE = new StorageServiceRegistry();

  private static final ExtensionPointHandler HANDLER = new ExtensionPointHandler();

  private static final String SERVICE_LABEL = "serviceLabel";

  private static final String SERVICE_URI = "serviceURI";

  private static final String CREATE_ACCOUNT_URI = "createAccountURI";

  private static final String EDIT_ACCOUNT_URI = "editAccountURI";

  private static final String RECOVER_PASSWORD_URI = "recoverPasswordURI";

  private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

  private final Set<WeakReference<Storage>> storages = new HashSet<WeakReference<Storage>>();

  private final Map<URI, IStorageService> services = new LinkedHashMap<URI, IStorageService>();

  private boolean running;

  private StorageServiceRegistry()
  {
  }

  @Override
  public IStorageService[] getServices()
  {
    synchronized (services)
    {
      start();
      return services.values().toArray(new IStorageService[services.size()]);
    }
  }

  @Override
  public StorageService getService(URI serviceURI)
  {
    synchronized (services)
    {
      start();
      return (StorageService)services.get(serviceURI);
    }
  }

  public StorageService getFirstService()
  {
    synchronized (services)
    {
      start();

      if (services.isEmpty())
      {
        return null;
      }

      return (StorageService)services.values().iterator().next();
    }
  }

  @Override
  public IStorageService.Dynamic addService(String serviceLabel, URI serviceURI, URI createAccountURI, URI editAccountURI, URI recoverPasswordURI)
      throws IllegalStateException
  {
    DynamicService service = new DynamicService(serviceLabel, serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
    addService(service);

    ISecurePreferences securePreferences = service.getSecurePreferences();
    if (securePreferences != null)
    {
      try
      {
        securePreferences.put(SERVICE_LABEL, serviceLabel, false);
        securePreferences.put(SERVICE_URI, serviceURI.toString(), false);
        setSecurePreference(securePreferences, CREATE_ACCOUNT_URI, createAccountURI);
        setSecurePreference(securePreferences, EDIT_ACCOUNT_URI, editAccountURI);
        setSecurePreference(securePreferences, RECOVER_PASSWORD_URI, recoverPasswordURI);
        securePreferences.flush();
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }

    return service;
  }

  @Override
  public IStorageService.Dynamic addService(String serviceLabel, URI serviceURI) throws IllegalStateException
  {
    return addService(serviceLabel, serviceURI, null, null, null);
  }

  @Override
  public IStorageService.Dynamic[] refresh()
  {
    List<IStorageService.Dynamic> result = new ArrayList<IStorageService.Dynamic>();

    ISecurePreferences securePreferences = Activator.getSecurePreferences();
    if (securePreferences != null)
    {
      for (String name : securePreferences.childrenNames())
      {
        try
        {
          ISecurePreferences child = securePreferences.node(name);

          String serviceLabel = child.get(SERVICE_LABEL, null);
          if (StringUtil.isEmpty(serviceLabel))
          {
            continue;
          }

          URI serviceURI = StringUtil.newURI(child.get(SERVICE_URI, null));
          if (serviceURI == null || !StringUtil.encodeURI(serviceURI).equals(name))
          {
            continue;
          }

          URI createAccountURI = StringUtil.newURI(child.get(CREATE_ACCOUNT_URI, null));
          URI editAccountURI = StringUtil.newURI(child.get(EDIT_ACCOUNT_URI, null));
          URI recoverPasswordURI = StringUtil.newURI(child.get(RECOVER_PASSWORD_URI, null));

          IStorageService.Dynamic service = new DynamicService(serviceLabel, serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
          addService(service);
          result.add(service);
        }
        catch (Exception ex)
        {
          //$FALL-THROUGH$
        }
      }
    }

    return result.toArray(new IStorageService.Dynamic[result.size()]);
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

  void addStorage(Storage storage)
  {
    synchronized (storages)
    {
      storages.add(new WeakReference<Storage>(storage));
    }
  }

  void addService(IStorageService service) throws IllegalStateException
  {
    URI serviceURI = service.getServiceURI();

    synchronized (services)
    {
      start();

      IStorageService registered = services.get(serviceURI);
      if (registered != null)
      {
        throw new IllegalStateException("Service already registered: " + registered);
      }

      services.put(serviceURI, service);
    }

    for (Listener listener : listeners)
    {
      try
      {
        listener.serviceAdded(service);
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }
  }

  void removeService(IStorageService service)
  {
    URI serviceURI = service.getServiceURI();

    synchronized (services)
    {
      start();
      services.remove(serviceURI);
    }

    for (Listener listener : listeners)
    {
      try
      {
        listener.serviceRemoved(service);
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }

    List<Storage> storagesToNotify = new ArrayList<Storage>();
    synchronized (storages)
    {
      for (Iterator<WeakReference<Storage>> it = storages.iterator(); it.hasNext();)
      {
        WeakReference<Storage> ref = it.next();
        Storage storage = ref.get();
        if (storage != null)
        {
          storagesToNotify.add(storage);
        }
        else
        {
          it.remove();
        }
      }
    }

    for (Storage storage : storagesToNotify)
    {
      storage.serviceRemoved(service);
    }
  }

  void start()
  {
    synchronized (services)
    {
      if (!running)
      {
        running = true;

        try
        {
          URI serviceURI = StringUtil.newURI("https://api.eclipse.org/");
          URI createAccountURI = StringUtil.newURI("https://dev.eclipse.org/site_login/");
          URI editAccountURI = StringUtil.newURI("https://dev.eclipse.org/site_login/myaccount.php");
          URI recoverPasswordURI = StringUtil.newURI("https://dev.eclipse.org/site_login/password_recovery.php");

          StorageService eclipseStorage = new StorageService("Eclipse.org", serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
          services.put(eclipseStorage.getServiceURI(), eclipseStorage);

          if (Activator.PLATFORM_RUNNING)
          {
            HANDLER.start();
          }

          refresh();
        }
        catch (Exception ex)
        {
          Activator.log(ex);
        }
      }
    }
  }

  void stop() throws Exception
  {
    synchronized (services)
    {
      if (running)
      {
        running = false;

        try
        {
          if (Activator.PLATFORM_RUNNING)
          {
            HANDLER.stop();
          }

          services.clear();
        }
        catch (Exception ex)
        {
          Activator.log(ex);
        }
      }
    }
  }

  private static void setSecurePreference(ISecurePreferences securePreferences, String key, URI uri) throws StorageException
  {
    if (uri != null)
    {
      securePreferences.put(key, uri.toString(), false);
    }
    else
    {
      securePreferences.remove(key);
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
        INSTANCE.addService(storage);
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
        INSTANCE.removeService(storage);
      }
      catch (Exception ex)
      {
        Activator.log(ex);
      }
    }

    private StorageService createStorage(IConfigurationElement configurationElement)
    {
      String serviceLabel = configurationElement.getAttribute(SERVICE_LABEL);
      URI serviceURI = StringUtil.newURI(configurationElement.getAttribute(SERVICE_URI));
      URI createAccountURI = StringUtil.newURI(configurationElement.getAttribute(CREATE_ACCOUNT_URI));
      URI editAccountURI = StringUtil.newURI(configurationElement.getAttribute(EDIT_ACCOUNT_URI));
      URI recoverPasswordURI = StringUtil.newURI(configurationElement.getAttribute(RECOVER_PASSWORD_URI));

      return new StorageService(serviceLabel, serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
    }
  }
}
