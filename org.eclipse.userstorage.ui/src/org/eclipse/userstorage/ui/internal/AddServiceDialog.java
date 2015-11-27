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
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.ui.AbstractDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Eike Stepper
 */
public class AddServiceDialog extends AbstractDialog implements ModifyListener
{
  private Text serviceLabelText;

  private Text serviceURIText;

  private Text createAccountURIText;

  private Text editAccountURIText;

  private Text recoverPasswordURIText;

  private Button okButton;

  private String serviceLabel;

  private URI serviceURI;

  private URI createAccountURI;

  private URI editAccountURI;

  private URI recoverPasswordURI;

  public AddServiceDialog(Shell parentShell)
  {
    super(parentShell);
  }

  public String getServiceLabel()
  {
    return serviceLabel;
  }

  public URI getServiceURI()
  {
    return serviceURI;
  }

  public URI getCreateAccountURI()
  {
    return createAccountURI;
  }

  public URI getEditAccountURI()
  {
    return editAccountURI;
  }

  public URI getRecoverPasswordURI()
  {
    return recoverPasswordURI;
  }

  @Override
  protected void configureShell(Shell newShell)
  {
    super.configureShell(newShell);
    newShell.setText("User Storage Service");
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    setTitle("Add Service");
    setMessage("Enter a service label and a unique service URI.");
    initializeDialogUnits(parent);

    Composite area = (Composite)super.createDialogArea(parent);

    GridLayout containerGridLayout = new GridLayout();
    containerGridLayout.numColumns = 2;

    Composite container = new Composite(area, SWT.NONE);
    container.setLayoutData(new GridData(GridData.FILL_BOTH));
    container.setLayout(containerGridLayout);

    Label serviceLabelLabel = new Label(container, SWT.NONE);
    serviceLabelLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    serviceLabelLabel.setText("Service label:");

    serviceLabelText = new Text(container, SWT.BORDER);
    serviceLabelText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    serviceLabelText.addModifyListener(this);

    Label serviceURILabel = new Label(container, SWT.NONE);
    serviceURILabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    serviceURILabel.setText("Service URI:");

    serviceURIText = new Text(container, SWT.BORDER);
    serviceURIText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    serviceURIText.addModifyListener(this);

    Label createAccountURILabel = new Label(container, SWT.NONE);
    createAccountURILabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    createAccountURILabel.setText("Create account URI:");

    createAccountURIText = new Text(container, SWT.BORDER);
    createAccountURIText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    createAccountURIText.addModifyListener(this);

    Label editAccountURILabel = new Label(container, SWT.NONE);
    editAccountURILabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    editAccountURILabel.setText("Edit account URI:");

    editAccountURIText = new Text(container, SWT.BORDER);
    editAccountURIText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    editAccountURIText.addModifyListener(this);

    Label recoverPasswordURILabel = new Label(container, SWT.NONE);
    recoverPasswordURILabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    recoverPasswordURILabel.setText("Recover password URI:");

    recoverPasswordURIText = new Text(container, SWT.BORDER);
    recoverPasswordURIText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    recoverPasswordURIText.addModifyListener(this);
    return area;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent)
  {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    okButton.setEnabled(false);

    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  @Override
  protected Point getInitialSize()
  {
    return new Point(450, 300);
  }

  @Override
  public void modifyText(ModifyEvent e)
  {
    okButton.setEnabled(true);
    setErrorMessage(null);

    serviceLabel = serviceLabelText.getText();

    try
    {
      String text = serviceURIText.getText();
      if (StringUtil.isEmpty(text))
      {
        serviceURI = null;
      }
      else
      {
        serviceURI = new URI(text);

        if (IStorageService.Registry.INSTANCE.getService(serviceURI) != null)
        {
          setErrorMessage("The service URI is not unique.");
          okButton.setEnabled(false);
          return;
        }
      }
    }
    catch (URISyntaxException ex)
    {
      setErrorMessage("The service URI is invalid.");
      okButton.setEnabled(false);
      return;
    }

    try
    {
      String text = createAccountURIText.getText();
      createAccountURI = StringUtil.isEmpty(text) ? null : new URI(text);
    }
    catch (URISyntaxException ex)
    {
      setErrorMessage("The create account URI is invalid.");
      okButton.setEnabled(false);
      return;
    }

    try
    {
      String text = editAccountURIText.getText();
      editAccountURI = StringUtil.isEmpty(text) ? null : new URI(text);
    }
    catch (URISyntaxException ex)
    {
      setErrorMessage("The edit account URI is invalid.");
      okButton.setEnabled(false);
      return;
    }

    try
    {
      String text = recoverPasswordURIText.getText();
      recoverPasswordURI = StringUtil.isEmpty(text) ? null : new URI(text);
    }
    catch (URISyntaxException ex)
    {
      setErrorMessage("The recover password URI is invalid.");
      okButton.setEnabled(false);
      return;
    }

    if (StringUtil.isEmpty(serviceLabel))
    {
      okButton.setEnabled(false);
      return;
    }

    if (serviceURI == null)
    {
      okButton.setEnabled(false);
      return;
    }
  }
}
