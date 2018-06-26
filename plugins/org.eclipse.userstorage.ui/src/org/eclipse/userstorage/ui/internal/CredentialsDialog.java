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
import org.eclipse.userstorage.internal.StorageService;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.spi.Credentials;
import org.eclipse.userstorage.ui.AbstractDialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
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

  private final boolean reauthentication;

  private Credentials credentials;

  private CredentialsComposite credentialsComposite;

  private Button okButton;

  public CredentialsDialog(Shell parentShell, IStorageService service, boolean reauthentication)
  {
    super(parentShell);
    this.service = service;
    this.reauthentication = reauthentication;
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
  protected IDialogSettings getPluginSettings()
  {
    return Activator.getDefault().getDialogSettings();
  }

  @Override
  protected void configureShell(Shell newShell)
  {
    super.configureShell(newShell);

    String shellText = "User Storage Service";

    String authority = service.getServiceURI().getAuthority();
    if (authority != null && authority.endsWith(".eclipse.org"))
    {
      shellText = "Eclipse " + shellText;
    }

    newShell.setText(shellText);
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    setTitle("Login");
    if (reauthentication)
    {
      setErrorMessage("You could not be logged in to your " + service.getServiceLabel() + " account. Please try again.");
    }
    else
    {
      setMessage("Enter the login information for your " + service.getServiceLabel() + " account.");
    }

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
    ((StorageService)service).setTermsOfUseAgreed(credentialsComposite.isTermsOfUseAgreed());
    credentials = credentialsComposite.getCredentials();
    super.okPressed();
  }

  protected boolean isPageValid()
  {
    String termsOfUseLink = service.getTermsOfUseLink();
    if (!StringUtil.isEmpty(termsOfUseLink))
    {
      boolean termsOfUseAgreed = credentialsComposite.isTermsOfUseAgreed();
      if (!termsOfUseAgreed)
      {
        return false;
      }
    }

    Credentials credentials = credentialsComposite.getCredentials();
    if (credentials == null)
    {
      return false;
    }

    if (StringUtil.isEmpty(credentials.getUsername()))
    {
      return false;
    }

    if (StringUtil.isEmpty(credentials.getPassword()))
    {
      return false;
    }

    return true;
  }

  private void validatePage()
  {
    if (okButton != null)
    {
      boolean valid = credentialsComposite.isValid();
      okButton.setEnabled(valid);
    }
  }
}
