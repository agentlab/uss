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
package org.eclipse.userstorage.ui;

import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.internal.StorageServiceRegistry;

import org.eclipse.swt.widgets.Composite;

/**
 * @author Eike Stepper
 */
public class StorageConfigurationComposite extends ServiceSelectorComposite
{
  private final IStorage storage;

  private IStorageService defaultService;

  public StorageConfigurationComposite(Composite parent, int style, IStorage storage)
  {
    super(parent, style);
    this.storage = storage;

    defaultService = storage.getService();
    performDefaults();
  }

  public final IStorage getStorage()
  {
    return storage;
  }

  public final boolean isDirty()
  {
    IStorageService selectedService = getSelectedService();
    return selectedService != defaultService;
  }

  public final boolean performDefaults()
  {
    if (isDirty())
    {
      setSelectedService(defaultService);
      return true;
    }

    return false;
  }

  public final boolean performApply()
  {
    if (isDirty())
    {
      defaultService = getSelectedService();
      storage.setService(defaultService);
      return true;
    }

    return false;
  }

  @Override
  protected void serviceRemoved(IStorageService service)
  {
    IStorageService selectedService = getSelectedService();
    if (service == selectedService)
    {
      service = StorageServiceRegistry.INSTANCE.getFirstService();
      setSelectedService(service);
    }
  }
}
