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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.StringTokenizer;

/**
 * @author Eike Stepper
 */
public abstract class SystemBrowser
{
  private static final String[] NO_COMMANDS = {};

  // Don't use "explorer" as it forks another process and returns a confusing exit value.
  private static final String[] WIN_COMMANDS = NO_COMMANDS;

  private static final String[] MAC_COMMANDS = { "open" };

  private static final String[] LINUX_COMMANDS = { "kde-open", "gnome-open", "xdg-open", "sensible-browser" };

  public static boolean open(String url)
  {
    try
    {
      for (String command : getOpenCommands())
      {
        if (open(command, url))
        {
          return true;
        }
      }
    }
    catch (Throwable ex)
    {
      //$FALL-THROUGH$
    }

    try
    {
      java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
      desktop.browse(new URI(url));
      return true;
    }
    catch (Throwable ex)
    {
      Activator.log(ex, IStatus.WARNING);
    }

    return false;
  }

  private static boolean open(String command, String url)
  {
    if (getFromPath(command) != null)
    {
      String[] cmdarray = { command, url };

      try
      {
        Process process = Runtime.getRuntime().exec(cmdarray);
        if (process != null)
        {
          // Don't check whether the process is still running; some commands just delegate to others and terminate.
          return true;
        }
      }
      catch (IOException ex)
      {
        //$FALL-THROUGH$
      }
    }

    return false;
  }

  private static String[] getOpenCommands()
  {
    String os = Platform.getOS();
    if (Platform.OS_WIN32.equals(os))
    {
      return WIN_COMMANDS;
    }

    if (Platform.OS_MACOSX.equals(os))
    {
      return MAC_COMMANDS;
    }

    if (Platform.OS_LINUX.equals(os))
    {
      return LINUX_COMMANDS;
    }

    return NO_COMMANDS;
  }

  private static File getFromPath(String command)
  {
    String path = System.getenv().get("PATH");

    StringTokenizer tokenizer = new StringTokenizer(path, File.pathSeparator);
    while (tokenizer.hasMoreTokens())
    {
      String folder = tokenizer.nextToken();
      File file = new File(folder, command);
      if (file.isFile())
      {
        return file;
      }
    }

    return null;
  }
}
