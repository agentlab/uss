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

import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.IStorageService.Registry;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;

import java.util.Arrays;

/**
 * @author Eike Stepper
 */
public class ServicesContentProvider implements IStructuredContentProvider, IStorageService.Registry.Listener
{
  private static final Object[] NO_ELEMENTS = {};

  private Viewer viewer;

  private IStorageService.Registry registry;

  public ServicesContentProvider()
  {
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
  {
    dispose();

    this.viewer = viewer;
    registry = (Registry)newInput;

    if (registry != null)
    {
      registry.addListener(this);
    }
  }

  @Override
  public void serviceAdded(IStorageService service)
  {
    refreshViewer();
  }

  @Override
  public void serviceRemoved(IStorageService service)
  {
    refreshViewer();
  }

  @Override
  public Object[] getElements(Object inputElement)
  {
    if (registry != null)
    {
      IStorageService[] services = registry.getServices();
      Arrays.sort(services);
      return services;
    }

    return NO_ELEMENTS;
  }

  @Override
  public void dispose()
  {
    if (registry != null)
    {
      registry.removeListener(this);
    }
  }

  private void refreshViewer()
  {
    if (viewer != null)
    {
      final Control control = viewer.getControl();
      control.getDisplay().asyncExec(new Runnable()
      {
        @Override
        public void run()
        {
          if (!control.isDisposed())
          {
            viewer.refresh();
          }
        }
      });
    }
  }
}
