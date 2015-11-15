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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Eike Stepper
 */
public class CredentialsDialog extends TitleAreaDialog implements ModifyListener
{
  private static final String TITLE = "User Storage Login";

  private IStorageService storage;

  private Credentials credentials;

  private CredentialsComposite credentialsComposite;

  private Button okButton;

  public CredentialsDialog(Shell parentShell, IStorageService storage)
  {
    super(parentShell);
    this.storage = storage;

    setShellStyle(SWT.SHELL_TRIM | SWT.BORDER | SWT.APPLICATION_MODAL);
  }

  public IStorageService getStorage()
  {
    return storage;
  }

  public Credentials getCredentials()
  {
    return credentials;
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Composite area = (Composite)super.createDialogArea(parent);

    Shell shell = getShell();

    ImageDescriptor descriptor = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/LoginBanner.png");
    final Image titleImage = descriptor.createImage(shell.getDisplay());

    shell.addDisposeListener(new DisposeListener()
    {
      @Override
      public void widgetDisposed(DisposeEvent e)
      {
        titleImage.dispose();
      }
    });

    shell.setText(TITLE);
    setTitle(TITLE);
    setTitleImage(titleImage);
    setMessage("Enter the credentials for your Eclipse.org account.");
    initializeDialogUnits(parent);

    credentialsComposite = new CredentialsComposite(area, SWT.NONE, 10, 10)
    {
      @Override
      protected void validate()
      {
        validatePage();
      }
    };

    credentialsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    credentialsComposite.setStorage(storage);
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
  public void modifyText(ModifyEvent e)
  {
    validatePage();
  }

  @Override
  protected void okPressed()
  {
    credentials = credentialsComposite.getCredentials();
    super.okPressed();
  }

  private void validatePage()
  {
    if (okButton != null)
    {
      Credentials credentials = credentialsComposite.getCredentials();
      okButton.setEnabled(credentials != null);
    }
  }
}
