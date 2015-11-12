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

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * @author Eike Stepper
 */
public final class UIUtil
{
  private UIUtil()
  {
  }

  public static Display getDisplay()
  {
    Display display = Display.getCurrent();
    if (display == null)
    {
      try
      {
        display = PlatformUI.getWorkbench().getDisplay();
      }
      catch (Throwable ignore)
      {
        //$FALL-THROUGH$
      }
    }

    if (display == null)
    {
      display = Display.getDefault();
    }

    if (display == null)
    {
      display = new Display();
    }

    return display;
  }

  public static Shell getShell()
  {
    final Shell[] shell = { null };

    final Display display = getDisplay();
    display.syncExec(new Runnable()
    {
      @Override
      public void run()
      {
        shell[0] = display.getActiveShell();

        if (shell[0] == null)
        {
          try
          {
            IWorkbench workbench = PlatformUI.getWorkbench();

            IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
            if (window == null)
            {
              IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
              if (windows.length != 0)
              {
                window = windows[0];
              }
            }

            if (window != null)
            {
              shell[0] = window.getShell();
            }
          }
          catch (Throwable ignore)
          {
            //$FALL-THROUGH$
          }
        }

        if (shell[0] == null)
        {
          Shell[] shells = display.getShells();
          if (shells.length > 0)
          {
            shell[0] = shells[0];
          }
        }
      }
    });

    return shell[0];
  }

  public static GridLayout createGridLayout(int numColumns)
  {
    GridLayout layout = new GridLayout(numColumns, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    return layout;
  }
}
