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
package org.eclipse.userstorage.ui.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.osgi.framework.BundleContext;

/**
 * @author Eike Stepper
 */
public class Activator extends AbstractUIPlugin
{
  public static final String PLUGIN_ID = "org.eclipse.userstorage.ui"; //$NON-NLS-1$

  private static Activator plugin;

  public Activator()
  {
  }

  @Override
  public void start(BundleContext context) throws Exception
  {
    super.start(context);
    plugin = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception
  {
    plugin = null;
    super.stop(context);
  }

  public static Activator getDefault()
  {
    return plugin;
  }

  public static void log(IStatus status)
  {
    getDefault().getLog().log(status);
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
    if (msg == null || msg.length() == 0)
    {
      msg = t.getClass().getName();
    }

    return new Status(severity, PLUGIN_ID, msg, t);
  }
}
