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

import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.ui.internal.ServicesContentProvider;
import org.eclipse.userstorage.ui.internal.ServicesPreferencePage;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * @author Eike Stepper
 */
public class ServiceSelectorComposite extends Composite
{
  private final StructuredViewer viewer;

  private final Link link;

  private IStorageService selectedService;

  public ServiceSelectorComposite(Composite parent, int style)
  {
    super(parent, style);

    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    setLayout(gridLayout);

    ServicesContentProvider contentProvider = new ServicesContentProvider()
    {
      @Override
      public void serviceAdded(IStorageService service)
      {
        super.serviceAdded(service);
        ServiceSelectorComposite.this.serviceAdded(service);
      }

      @Override
      public void serviceRemoved(IStorageService service)
      {
        super.serviceRemoved(service);
        ServiceSelectorComposite.this.serviceRemoved(service);
      }
    };

    viewer = createViewer(this);
    viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    viewer.setContentProvider(contentProvider);
    viewer.setInput(IStorageService.Registry.INSTANCE);
    viewer.addSelectionChangedListener(new ISelectionChangedListener()
    {
      @Override
      public void selectionChanged(SelectionChangedEvent event)
      {
        selectedService = getViewerSelection();
      }
    });

    link = new Link(this, SWT.NONE);
    link.setText("<a>Configure</a>");
    link.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        IStorageService service = getViewerSelection();

        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), ServicesPreferencePage.ID, null, service);
        dialog.open();
      }
    });
  }

  public final IStorageService getSelectedService()
  {
    return selectedService;
  }

  public final void setSelectedService(IStorageService service)
  {
    if (service != selectedService)
    {
      selectedService = service;
      viewer.setSelection(new StructuredSelection(service));
    }
  }

  @Override
  public boolean setFocus()
  {
    return viewer.getControl().setFocus();
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    super.setEnabled(enabled);
    viewer.getControl().setEnabled(enabled);
    link.setEnabled(enabled);
  }

  protected void serviceAdded(IStorageService service)
  {
    // Do nothing.
  }

  protected void serviceRemoved(IStorageService service)
  {
    // Do nothing.
  }

  protected StructuredViewer createViewer(Composite parent)
  {
    return new ComboViewer(parent, SWT.READ_ONLY);
  }

  private IStorageService getViewerSelection()
  {
    IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
    return (IStorageService)selection.getFirstElement();
  }

  public static boolean isShowServices()
  {
    String property = System.getProperty("org.eclipse.userstorage.ui.showServices", "auto");
    if ("auto".equalsIgnoreCase(property))
    {
      return IStorageService.Registry.INSTANCE.getServices().length > 1;
    }
  
    return Boolean.parseBoolean(property);
  }
}
