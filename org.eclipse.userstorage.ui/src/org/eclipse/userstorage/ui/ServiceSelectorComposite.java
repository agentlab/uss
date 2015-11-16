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
  private final ComboViewer comboViewer;

  private final Link link;

  private IStorageService selectedService;

  public ServiceSelectorComposite(Composite parent, int style)
  {
    super(parent, style);

    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    setLayout(gridLayout);

    comboViewer = new ComboViewer(this, SWT.NONE);
    comboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    comboViewer.setContentProvider(new ServicesContentProvider());
    comboViewer.setInput(IStorageService.Registry.INSTANCE);
    comboViewer.addSelectionChangedListener(new ISelectionChangedListener()
    {
      @Override
      public void selectionChanged(SelectionChangedEvent event)
      {
        IStructuredSelection selection = (IStructuredSelection)event.getSelection();
        selectedService = (IStorageService)selection.getFirstElement();
      }
    });

    link = new Link(this, SWT.NONE);
    link.setText("<a>Configure</a>");
    link.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        IStructuredSelection selection = (IStructuredSelection)comboViewer.getSelection();
        IStorageService service = (IStorageService)selection.getFirstElement();

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
    selectedService = service;
    comboViewer.setSelection(new StructuredSelection(service));
  }

  @Override
  public boolean setFocus()
  {
    return comboViewer.getControl().setFocus();
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    super.setEnabled(enabled);
    comboViewer.getControl().setEnabled(enabled);
    link.setEnabled(enabled);
  }
}
