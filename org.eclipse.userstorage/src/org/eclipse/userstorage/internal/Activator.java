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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.spi.ICredentialsProvider;

import org.osgi.framework.BundleContext;

/**
 * @author Eike Stepper
 */
public class Activator extends Plugin
{
  public static final String PLUGIN_ID = "org.eclipse.userstorage"; //$NON-NLS-1$

  public static final boolean PLATFORM_RUNNING;

  static
  {
    boolean result = false;

    try
    {
      result = Platform.isRunning();
    }
    catch (Throwable exception)
    {
      // Assume that we aren't running.
    }

    PLATFORM_RUNNING = result;
  }

  private static Activator plugin;

  private static ICredentialsProvider credentialsProvider = new NOOPCredentialsProvider();

  @Override
  public void start(BundleContext bundleContext) throws Exception
  {
    super.start(bundleContext);
    plugin = this;

    start();
  }

  @Override
  public void stop(BundleContext context) throws Exception
  {
    stop();
    plugin = null;
    super.stop(context);
  }

  public static Activator getDefault()
  {
    return plugin;
  }

  public static void log(IStatus status)
  {
    if (plugin != null)
    {
      plugin.getLog().log(status);
    }
    else
    {
      if (status.getSeverity() == IStatus.ERROR)
      {
        Throwable exception = status.getException();
        if (exception != null)
        {
          exception.printStackTrace();
        }
        else
        {
          System.err.println(status);
        }
      }
      else
      {
        System.out.println(status);
      }
    }
  }

  public static void log(Throwable t, int severity)
  {
    log(getStatus(t, severity));
  }

  public static String log(Throwable t)
  {
    IStatus status = getStatus(t);
    log(status);
    return status.getMessage();
  }

  public static IStatus getStatus(Object obj)
  {
    if (obj instanceof CoreException)
    {
      CoreException coreException = (CoreException)obj;
      return coreException.getStatus();
    }

    if (obj instanceof Throwable)
    {
      Throwable t = (Throwable)obj;
      return getStatus(t, IStatus.ERROR);
    }

    return new Status(IStatus.INFO, PLUGIN_ID, obj.toString(), null);
  }

  public static IStatus getStatus(Throwable t, int severity)
  {
    String msg = t.getLocalizedMessage();
    if (StringUtil.isEmpty(msg))
    {
      msg = t.getClass().getName();
    }

    return new Status(severity, PLUGIN_ID, msg, t);
  }

  public static ICredentialsProvider getCredentialsProvider()
  {
    return credentialsProvider;
  }

  public static void start() throws Exception
  {
    if (PLATFORM_RUNNING)
    {
      initCredentialsProvider();
    }

    StorageRegistry.INSTANCE.start();
  }

  public static void stop() throws Exception
  {
    StorageRegistry.INSTANCE.stop();
    credentialsProvider = null;
  }

  private static void initCredentialsProvider()
  {
    try
    {
      IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
      IConfigurationElement prioritizedConfigurationElement = null;
      int highestPriority = Integer.MIN_VALUE;

      for (IConfigurationElement configurationElement : extensionRegistry.getConfigurationElementsFor(PLUGIN_ID + ".credentialsProviders"))
      {
        try
        {
          String priorityAttribute = configurationElement.getAttribute("priority");
          int priority = 500;

          try
          {
            priority = Integer.parseInt(priorityAttribute);
          }
          catch (NumberFormatException ex)
          {
            //$FALL-THROUGH$
          }

          if (priority > highestPriority)
          {
            prioritizedConfigurationElement = configurationElement;
            highestPriority = priority;
          }
        }
        catch (Exception ex)
        {
          log(ex);
        }
      }

      if (prioritizedConfigurationElement != null)
      {
        credentialsProvider = (ICredentialsProvider)prioritizedConfigurationElement.createExecutableExtension("class");
      }
    }
    catch (Exception ex)
    {
      log(ex);
    }
  }

  /**
   * @author Eike Stepper
   */
  private static final class NOOPCredentialsProvider implements ICredentialsProvider
  {
    @Override
    public Credentials provideCredentials(IStorage storage)
    {
      return null;
    }
  }
}
