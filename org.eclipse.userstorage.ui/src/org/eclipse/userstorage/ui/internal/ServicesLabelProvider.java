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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * @author Eike Stepper
 */
public class ServicesLabelProvider extends LabelProvider
{
  private final Image serviceImage = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/Service.gif").createImage();

  private final Image dynamicServiceImage = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/DynamicService.gif").createImage();

  public ServicesLabelProvider()
  {
  }

  @Override
  public Image getImage(Object element)
  {
    if (element instanceof IStorageService.Dynamic)
    {
      return dynamicServiceImage;
    }

    return serviceImage;
  }

  @Override
  public String getText(Object element)
  {
    return ((IStorageService)element).getServiceLabel();
  }

  @Override
  public void dispose()
  {
    dynamicServiceImage.dispose();
    serviceImage.dispose();
    super.dispose();
  }
}
