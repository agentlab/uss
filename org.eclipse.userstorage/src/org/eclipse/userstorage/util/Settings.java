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
package org.eclipse.userstorage.util;

import org.eclipse.userstorage.internal.Activator;
import org.eclipse.userstorage.internal.StorageProperties;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.spi.ISettings;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Eike Stepper
 */
public final class Settings
{
  public static final ISettings DEFAULT = createDefaultSettings();

  public static final ISettings NONE = new NoSettings();

  private Settings()
  {
  }

  private static ISettings createDefaultSettings()
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
      catch (Throwable ex)
      {
        //$FALL-THROUGH$
      }

      try
      {
        return new EclipseSettings("configuration");
      }
      catch (Throwable ex)
      {
        //$FALL-THROUGH$
      }
    }

    return new MemorySettings();
  }

  /**
   * @author Eike Stepper
   */
  private static final class NoSettings implements ISettings
  {
    public NoSettings()
    {
    }

    @Override
    public String getValue(String key) throws Exception
    {
      return null;
    }

    @Override
    public void setValue(String key, String value) throws Exception
    {
      // Do nothing.
    }
  }

  /**
   * @author Eike Stepper
   */
  public static final class MemorySettings implements ISettings
  {
    private final Map<String, String> map = new HashMap<String, String>();

    public MemorySettings()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue(String key) throws Exception
    {
      return map.get(key);
    }

    /**
     * {@inheritDoc}
     */
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

  /**
   * @author Eike Stepper
   */
  public static final class EclipseSettings implements ISettings
  {
    private final Preferences node;

    public EclipseSettings(String scope) throws BackingStoreException
    {
      IEclipsePreferences rootNode = Platform.getPreferencesService().getRootNode();
      if (!rootNode.nodeExists(scope))
      {
        throw new BackingStoreException("Invalid scope: " + scope);
      }

      String nodeName = getNodeName();
      node = rootNode.node(scope).node(nodeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue(String key) throws Exception
    {
      return node.get(key, null);
    }

    /**
     * {@inheritDoc}
     */
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

    protected String getNodeName()
    {
      return Activator.PLUGIN_ID;
    }
  }

  /**
   * @author Eike Stepper
   */
  public final class FileSettings implements ISettings
  {
    private final File file;

    public FileSettings(File file)
    {
      this.file = file;
    }

    public FileSettings()
    {
      this(new File(System.getProperty("user.home"), ".eclipse/" + Activator.PLUGIN_ID + "/.settings"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue(String key) throws Exception
    {
      Properties properties = new Properties();

      if (file.isFile())
      {
        InputStream in = null;

        try
        {
          in = new FileInputStream(file);
          properties.load(in);
        }
        finally
        {
          IOUtil.close(in);
        }
      }

      return properties.getProperty(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(String key, String value) throws Exception
    {
      Properties properties = new Properties();

      if (file.isFile())
      {
        InputStream in = null;

        try
        {
          in = new FileInputStream(file);
          properties.load(in);
        }
        finally
        {
          IOUtil.close(in);
        }
      }

      boolean changed = false;
      if (value == null)
      {
        Object oldValue = properties.remove(key);
        if (oldValue != null)
        {
          changed = true;
        }
      }
      else
      {
        Object oldValue = properties.setProperty(key, value);
        if (oldValue != null && !oldValue.equals(value))
        {
          changed = true;
        }
      }

      if (changed)
      {
        OutputStream out = null;

        try
        {
          out = new FileOutputStream(file);
          properties.store(out, null);
        }
        finally
        {
          IOUtil.close(out);
        }
      }
    }
  }
}
