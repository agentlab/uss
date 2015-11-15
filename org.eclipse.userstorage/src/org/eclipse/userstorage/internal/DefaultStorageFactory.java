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

import org.eclipse.userstorage.spi.ISettings;
import org.eclipse.userstorage.spi.StorageFactory;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public final class DefaultStorageFactory extends StorageFactory
{
  private final ISettings settings = createSettings();

  public DefaultStorageFactory()
  {
  }

  @Override
  protected String getPreferredServiceURI(String applicationToken)
  {
    try
    {
      return settings.getValue(applicationToken);
    }
    catch (Exception ex)
    {
      Activator.log(ex);
    }

    return null;
  }

  @Override
  protected void setPreferredServiceURI(String applicationToken, String serviceURI)
  {
    try
    {
      settings.setValue(applicationToken, serviceURI);
    }
    catch (Exception ex)
    {
      Activator.log(ex);
    }
  }

  private static ISettings createSettings()
  {
    String property = System.getProperty(StorageProperties.SETTINGS, null);
    if (property != null)
    {
      try
      {
        @SuppressWarnings("unchecked")
        Class<ISettings> c = (Class<ISettings>)Class.forName(property);

        return c.newInstance();
      }
      catch (Throwable ex)
      {
        Activator.log(ex);
      }
    }

    if (Activator.PLATFORM_RUNNING)
    {
      try
      {
        return new EclipseSettings("instance");
      }
      catch (BackingStoreException ex)
      {
        //$FALL-THROUGH$
      }

      try
      {
        return new EclipseSettings("configuration");
      }
      catch (BackingStoreException ex)
      {
        //$FALL-THROUGH$
      }
    }

    return new StandaloneSettings();
  }

  /**
   * @author Eike Stepper
   */
  private static final class EclipseSettings implements ISettings
  {
    private final Preferences node;

    public EclipseSettings(String scope) throws BackingStoreException
    {
      IEclipsePreferences rootNode = Platform.getPreferencesService().getRootNode();
      if (!rootNode.nodeExists(scope))
      {
        throw new BackingStoreException("Invalid scope: " + scope);
      }

      node = rootNode.node(scope).node(Activator.PLUGIN_ID);
    }

    @Override
    public String getValue(String key) throws Exception
    {
      return node.get(key, null);
    }

    @Override
    public void setValue(String key, String value) throws Exception
    {
      if (value == null)
      {
        node.remove(key);
      }
      else
      {
        node.put(key, value);
      }

      node.flush();
    }
  }

  /**
   * @author Eike Stepper
   */
  private static final class StandaloneSettings implements ISettings
  {
    private final Map<String, String> map = new HashMap<String, String>();

    public StandaloneSettings()
    {
    }

    @Override
    public String getValue(String key) throws Exception
    {
      return map.get(key);
    }

    @Override
    public void setValue(String key, String value) throws Exception
    {
      if (value == null)
      {
        map.remove(key);
      }
      else
      {
        map.put(key, value);
      }
    }
  }
}
