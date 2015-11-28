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
import org.eclipse.userstorage.internal.Credentials;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.ui.internal.CredentialsComposite;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Eike Stepper
 */
public class CredentialsDialog extends AbstractDialog
{
  private final IStorageService service;

  private Credentials credentials;

  private CredentialsComposite credentialsComposite;

  private Button okButton;

  public CredentialsDialog(Shell parentShell, IStorageService service)
  {
    super(parentShell);
    this.service = service;
  }

  public final IStorageService getService()
  {
    return service;
  }

  public final Credentials getCredentials()
  {
    return credentials;
  }

  @Override
  protected void configureShell(Shell newShell)
  {
    super.configureShell(newShell);

    String shellText = AbstractDialog.createShellText(service);
    newShell.setText(shellText);
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    setTitle("Log-In");
    setMessage("Enter the log-in information for your '" + service.getServiceLabel() + "' account.");
    initializeDialogUnits(parent);

    Composite area = (Composite)super.createDialogArea(parent);

    credentialsComposite = new CredentialsComposite(area, SWT.NONE, 10, 10, true)
    {
      @Override
      protected void validate()
      {
        validatePage();
      }
    };

    credentialsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    credentialsComposite.setService(service);
    Dialog.applyDialogFont(credentialsComposite);

    return area;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent)
  {
    super.createButtonsForButtonBar(parent);
    okButton = getButton(IDialogConstants.OK_ID);
    validatePage();
  }

  @Override
  protected void okPressed()
  {
    credentials = credentialsComposite.getCredentials();
    super.okPressed();
  }

  protected void validatePage()
  {
    if (okButton != null)
    {
      Credentials credentials = credentialsComposite.getCredentials();
      okButton.setEnabled(credentials != null && !StringUtil.isEmpty(credentials.getUsername()) && !StringUtil.isEmpty(credentials.getPassword()));
    }
  }
}
