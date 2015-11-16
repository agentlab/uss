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
import org.eclipse.userstorage.internal.Credentials;
import org.eclipse.userstorage.internal.StorageService;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public class ServicesPreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage
{
  private final Map<IStorageService, Credentials> credentialsMap = new HashMap<IStorageService, Credentials>();

  private TableViewer servicesViewer;

  private CredentialsComposite credentialsComposite;

  private IStorageService currentService;

  private Button addButton;

  private Button removeButton;

  public ServicesPreferencePage()
  {
    super("User Storage");
  }

  @Override
  public void init(IWorkbench workbench)
  {
    // Do nothing.
  }

  private Composite createArea(Composite parent, int columns)
  {
    GridLayout layout = new GridLayout(columns, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;

    final Composite main = new Composite(parent, SWT.NONE);
    main.setLayout(layout);
    return main;
  }

  @Override
  protected Control createContents(final Composite parent)
  {
    final Composite mainArea = createArea(parent, 2);
    mainArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Composite leftArea = createArea(mainArea, 1);
    leftArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Label servicesLabel = new Label(leftArea, SWT.NONE);
    servicesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    servicesLabel.setText("Services:");

    final ServicesContentProvider contentProvider = new ServicesContentProvider();

    TableColumnLayout tableLayout = new TableColumnLayout();
    Composite tableComposite = new Composite(leftArea, SWT.NONE);
    tableComposite.setLayout(tableLayout);
    tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    servicesViewer = new TableViewer(tableComposite, SWT.BORDER);
    servicesViewer.setContentProvider(contentProvider);
    servicesViewer.setLabelProvider(new ServicesLabelProvider());
    servicesViewer.setInput(IStorageService.Registry.INSTANCE);
    servicesViewer.addSelectionChangedListener(new ISelectionChangedListener()
    {
      @Override
      public void selectionChanged(SelectionChangedEvent event)
      {
        IStructuredSelection selection = (IStructuredSelection)event.getSelection();
        setCurrentService((IStorageService)selection.getFirstElement());
      }
    });

    TableColumn tableColumn = new TableColumn(servicesViewer.getTable(), SWT.LEFT);
    tableLayout.setColumnData(tableColumn, new ColumnWeightData(100));

    credentialsComposite = new CredentialsComposite(leftArea, SWT.NONE, 0, 0, false)
    {
      @Override
      protected void validate()
      {
        if (currentService != null)
        {
          credentialsMap.put(currentService, getCredentials());
        }
      }
    };

    credentialsComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Composite rightArea = createArea(mainArea, 1);
    rightArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

    new Label(rightArea, SWT.NONE);

    addButton = new Button(rightArea, SWT.NONE);
    addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    addButton.setText("Add...");
    addButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        AddServiceDialog dialog = new AddServiceDialog(getShell());
        if (dialog.open() == AddServiceDialog.OK)
        {
          String serviceLabel = dialog.getServiceLabel();
          URI serviceURI = dialog.getServiceURI();
          URI createAccountURI = dialog.getCreateAccountURI();
          URI editAccountURI = dialog.getEditAccountURI();
          URI recoverPasswordURI = dialog.getRecoverPasswordURI();

          IStorageService.Registry.INSTANCE.addService(serviceLabel, serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
        }
      }
    });

    removeButton = new Button(rightArea, SWT.NONE);
    removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    removeButton.setText("Remove");
    removeButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        if (currentService instanceof IStorageService.Dynamic)
        {
          IStorageService.Dynamic dynamicService = (IStorageService.Dynamic)currentService;
          Object[] elements = contentProvider.getElements(null);
          final int currentIndex = getCurrentIndex(elements, dynamicService);

          if (MessageDialog.openQuestion(getShell(), "Remove Service", "Do you really want to remove the '" + dynamicService.getServiceLabel() + "' service?"))
          {
            dynamicService.remove();

            final Control control = servicesViewer.getControl();
            control.getDisplay().asyncExec(new Runnable()
            {
              @Override
              public void run()
              {
                if (!control.isDisposed())
                {
                  Object[] elements = contentProvider.getElements(null);
                  if (elements.length != 0)
                  {
                    int newIndex = currentIndex;
                    if (newIndex >= elements.length)
                    {
                      newIndex = elements.length - 1;
                    }

                    setCurrentService((IStorageService)elements[newIndex]);
                    servicesViewer.setSelection(new StructuredSelection(currentService));
                  }
                }
              }
            });
          }
        }
      }

      private int getCurrentIndex(Object[] elements, IStorageService service)
      {
        for (int i = 0; i < elements.length; i++)
        {
          Object element = elements[i];
          if (element == service)
          {
            return i;
          }
        }

        return 0;
      }
    });

    Button refreshButton = new Button(rightArea, SWT.NONE);
    refreshButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    refreshButton.setText("Refresh");
    refreshButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        IStorageService.Registry.INSTANCE.refresh();
      }
    });

    Object[] elements = contentProvider.getElements(null);
    if (elements.length != 0)
    {
      setCurrentService((IStorageService)elements[0]);
      servicesViewer.setSelection(new StructuredSelection(currentService));
    }

    return mainArea;
  }

  @Override
  protected void performDefaults()
  {
    credentialsMap.clear();

    IStorageService service = currentService;
    currentService = null;
    setCurrentService(service);

    super.performDefaults();
  }

  @Override
  public boolean performOk()
  {
    for (Map.Entry<IStorageService, Credentials> entry : credentialsMap.entrySet())
    {
      IStorageService service = entry.getKey();
      Credentials credentials = entry.getValue();
      ((StorageService)service).setCredentials(credentials);
    }

    return super.performOk();
  }

  private void setCurrentService(IStorageService service)
  {
    if (service != currentService)
    {
      currentService = service;

      if (currentService != null)
      {
        Credentials credentials = credentialsMap.get(currentService);
        if (credentials == null)
        {
          credentials = ((StorageService)currentService).getCredentials();
          if (credentials != null)
          {
            credentialsMap.put(currentService, credentials);
          }
        }

        credentialsComposite.setService(currentService);
        credentialsComposite.setCredentials(credentials);
        removeButton.setEnabled(currentService instanceof IStorageService.Dynamic);
      }
      else
      {
        credentialsComposite.setService(null);
        credentialsComposite.setCredentials(null);
        removeButton.setEnabled(false);
      }
    }
  }
}
