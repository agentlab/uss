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

import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.IStorageService.Registry;
import org.eclipse.userstorage.StorageFactory;
import org.eclipse.userstorage.internal.Credentials;
import org.eclipse.userstorage.internal.StorageService;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.ui.ServiceSelectorComposite;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
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
import java.util.Map.Entry;

/**
 * @author Eike Stepper
 */
public class ServicesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
  public static final String ID = "org.eclipse.userstorage.ui.ServicesPreferencePage";

  private static final Registry REGISTRY = IStorageService.Registry.INSTANCE;

  private Map<IStorageService, Credentials> credentialsMap = new HashMap<IStorageService, Credentials>();

  private Map<IStorageService, Boolean> termsOfUseAgreedMap = new HashMap<IStorageService, Boolean>();

  private TableViewer servicesViewer;

  private CredentialsComposite credentialsComposite;

  private Button addButton;

  private Button removeButton;

  private Button testButton;

  private IStorageService selectedService;

  private boolean performingDefaults;

  public ServicesPreferencePage()
  {
    super("User Storage Service");
  }

  @Override
  public void init(IWorkbench workbench)
  {
    // Do nothing.
  }

  @Override
  public void applyData(Object data)
  {
    if (data instanceof IStorageService)
    {
      IStorageService service = (IStorageService)data;
      setSelectedService(service);
    }
  }

  @Override
  public void createControl(Composite parent)
  {
    super.createControl(parent);
    updateEnablement();
  }

  @Override
  protected Control createContents(final Composite parent)
  {
    final ServicesContentProvider contentProvider = ServiceSelectorComposite.isShowServices() ? new ServicesContentProvider() : null;

    final Composite mainArea = createArea(parent, 2);
    mainArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Composite leftArea = createArea(mainArea, 1);
    leftArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    if (contentProvider != null)
    {
      Label servicesLabel = new Label(leftArea, SWT.NONE);
      servicesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      servicesLabel.setText("Services:");

      TableColumnLayout tableLayout = new TableColumnLayout();
      Composite tableComposite = new Composite(leftArea, SWT.NONE);
      tableComposite.setLayout(tableLayout);
      tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      servicesViewer = new TableViewer(tableComposite, SWT.BORDER);
      servicesViewer.setContentProvider(contentProvider);
      servicesViewer.setLabelProvider(new ServicesLabelProvider());
      servicesViewer.setInput(REGISTRY);
      servicesViewer.addSelectionChangedListener(new ISelectionChangedListener()
      {
        @Override
        public void selectionChanged(SelectionChangedEvent event)
        {
          IStructuredSelection selection = (IStructuredSelection)event.getSelection();
          setSelectedService((IStorageService)selection.getFirstElement());
        }
      });

      TableColumn tableColumn = new TableColumn(servicesViewer.getTable(), SWT.LEFT);
      tableLayout.setColumnData(tableColumn, new ColumnWeightData(100));

      new Label(leftArea, SWT.NONE);
    }

    credentialsComposite = new CredentialsComposite(leftArea, SWT.NONE, 0, 0, false)
    {
      @Override
      protected void validate()
      {
        if (selectedService != null && !performingDefaults)
        {
          Credentials credentials = getCredentials();
          credentialsMap.put(selectedService, credentials);

          boolean termsOfUseAgreed = isTermsOfUseAgreed();
          termsOfUseAgreedMap.put(selectedService, termsOfUseAgreed);

          updateEnablement();
        }
      }
    };

    credentialsComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    if (contentProvider != null)
    {
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
            String termsOfUseLink = dialog.getTermsOfUseLink();

            REGISTRY.addService(serviceLabel, serviceURI, createAccountURI, editAccountURI, recoverPasswordURI, termsOfUseLink);
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
          if (selectedService instanceof IStorageService.Dynamic)
          {
            IStorageService.Dynamic dynamicService = (IStorageService.Dynamic)selectedService;
            Object[] elements = contentProvider.getElements(null);
            final int currentIndex = getCurrentIndex(elements, dynamicService);

            if (MessageDialog.openQuestion(getShell(), "Remove Service",
                "Do you really want to remove the '" + dynamicService.getServiceLabel() + "' service?"))
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

                      setSelectedService((IStorageService)elements[newIndex]);
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
          REGISTRY.refresh();
        }
      });
    }

    Object[] elements = contentProvider == null ? REGISTRY.getServices() : contentProvider.getElements(null);
    if (elements.length != 0)
    {
      setSelectedService((IStorageService)elements[0]);
    }

    if (Boolean.getBoolean("org.eclipse.userstorage.ui.showTestButton"))
    {
      testButton = new Button(leftArea, SWT.PUSH);
      testButton.setText("Test");
      testButton.addSelectionListener(new SelectionAdapter()
      {
        @Override
        public void widgetSelected(SelectionEvent e)
        {
          try
          {
            IStorage storage = StorageFactory.DEFAULT.create("pDKTqBfDuNxlAKydhEwxBZPxa4q");
            IBlob blob = storage.getBlob("ui_test");
            blob.setContentsUTF("Test 123");
            performDefaults();
            MessageDialog.openInformation(getShell(), "Test", "Test succeeded.");
          }
          catch (Exception ex)
          {
            performDefaults();
            ErrorDialog.openError(getShell(), "Test", "Test failed.", Activator.getStatus(ex));
          }
        }
      });
    }

    return mainArea;
  }

  @Override
  protected void performDefaults()
  {
    credentialsMap.clear();
    termsOfUseAgreedMap.clear();

    try
    {
      performingDefaults = true;

      IStorageService service = selectedService;
      selectedService = null;
      setSelectedService(service);
    }
    finally
    {
      performingDefaults = false;
    }

    updateEnablement();
  }

  @Override
  protected Point doComputeSize()
  {
    return CredentialsComposite.INITIAL_SIZE;
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

    for (Entry<IStorageService, Boolean> entry : termsOfUseAgreedMap.entrySet())
    {
      IStorageService service = entry.getKey();
      Boolean termsOfUseAgreed = entry.getValue();
      ((StorageService)service).setTermsOfUseAgreed(Boolean.TRUE.equals(termsOfUseAgreed));
    }

    updateEnablement();
    return true;
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

  private void setSelectedService(IStorageService service)
  {
    if (service != selectedService)
    {
      selectedService = service;

      if (selectedService != null)
      {
        Credentials credentials = credentialsMap.get(selectedService);
        if (credentials == null)
        {
          credentials = ((StorageService)selectedService).getCredentials();
          if (credentials != null)
          {
            credentialsMap.put(selectedService, credentials);
          }
        }

        Boolean termsOfUseAgreed = termsOfUseAgreedMap.get(selectedService);
        if (termsOfUseAgreed == null)
        {
          termsOfUseAgreed = ((StorageService)selectedService).isTermsOfUseAgreed();
          termsOfUseAgreedMap.put(selectedService, termsOfUseAgreed);
        }

        credentialsComposite.setService(selectedService);
        credentialsComposite.setCredentials(credentials);
        credentialsComposite.setTermsOfUseAgreed(termsOfUseAgreed);

        if (removeButton != null)
        {
          removeButton.setEnabled(selectedService instanceof IStorageService.Dynamic);
        }

        if (servicesViewer != null)
        {
          servicesViewer.setSelection(new StructuredSelection(selectedService));
        }
      }
      else
      {
        credentialsComposite.setService(null);
        credentialsComposite.setCredentials(null);

        if (removeButton != null)
        {
          removeButton.setEnabled(false);
        }
      }
    }
  }

  private void updateEnablement()
  {
    boolean dirty = false;

    for (IStorageService service : REGISTRY.getServices())
    {
      Credentials localCredentials = credentialsMap.get(service);
      String localUsername = "";
      String localPassword = "";
      if (localCredentials != null)
      {
        localUsername = StringUtil.safe(localCredentials.getUsername());
        localPassword = StringUtil.safe(localCredentials.getPassword());
      }
      else
      {
        continue;
      }

      Credentials credentials = ((StorageService)service).getCredentials();
      String username = "";
      String password = "";
      if (credentials != null)
      {
        username = StringUtil.safe(credentials.getUsername());
        password = StringUtil.safe(credentials.getPassword());
      }

      if (!localUsername.equals(username) || !localPassword.equals(password))
      {
        dirty = true;
        break;
      }
    }

    if (!dirty)
    {
      for (IStorageService service : REGISTRY.getServices())
      {
        boolean localTermsOfUseAgreed = Boolean.TRUE.equals(termsOfUseAgreedMap.get(service));
        boolean termsOfUseAgreed = ((StorageService)service).isTermsOfUseAgreed();
        if (localTermsOfUseAgreed != termsOfUseAgreed)
        {
          dirty = true;
          break;
        }
      }
    }

    Button defaultsButton = getDefaultsButton();
    if (defaultsButton != null)
    {
      defaultsButton.setEnabled(dirty);
    }

    Button applyButton = getApplyButton();
    if (applyButton != null)
    {
      applyButton.setEnabled(dirty);
    }
  }
}
