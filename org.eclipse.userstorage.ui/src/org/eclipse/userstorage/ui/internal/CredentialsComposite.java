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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.internal.Credentials;
import org.eclipse.userstorage.internal.Storage;
import org.eclipse.userstorage.internal.util.StringUtil;

import java.net.URI;

/**
 * @author Eike Stepper
 */
public class CredentialsComposite extends Composite
{
  private final ModifyListener listener = new ModifyListener()
  {
    @Override
    public void modifyText(ModifyEvent e)
    {
      credentials = new Credentials(usernameText.getText(), passwordText.getText());
      validate();
    }
  };

  private IStorage storage;

  private Credentials credentials;

  private Label usernameLabel;

  private Text usernameText;

  private Label passwordLabel;

  private Text passwordText;

  private Link signupLink;

  public CredentialsComposite(Composite parent, int style, int marginWidth, int marginHeight)
  {
    super(parent, style);

    GridLayout layout = UIUtil.createGridLayout(getGridColumns());
    layout.marginWidth = marginWidth;
    layout.marginHeight = marginHeight;

    setLayout(layout);

    createUI(this, layout.numColumns);
  }

  public IStorage getStorage()
  {
    return storage;
  }

  public void setStorage(IStorage storage)
  {
    signupLink.setVisible(false);

    this.storage = storage;
    if (storage != null)
    {
      URI signupURI = storage.getCreateAccountURI();
      if (signupURI != null)
      {
        String label = getServiceLabel(storage);

        signupLink.setText("<a>Create an " + label + " account</a>");
        signupLink.setVisible(true);
      }

      setCredentials(((Storage)storage).getCredentials());
    }
  }

  public Credentials getCredentials()
  {
    return credentials;
  }

  public void setCredentials(Credentials credentials)
  {
    this.credentials = credentials;
    if (credentials != null)
    {
      usernameText.setText(StringUtil.safe(credentials.getUsername()));
      passwordText.setText(StringUtil.safe(credentials.getPassword()));
    }
    else
    {
      usernameText.setText("");
      passwordText.setText("");
    }
  }

  public int getGridColumns()
  {
    return 2;
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    usernameLabel.setEnabled(enabled);
    usernameText.setEnabled(enabled);
    passwordLabel.setEnabled(enabled);
    passwordText.setEnabled(enabled);
    signupLink.setEnabled(enabled);
  }

  protected void createUI(Composite parent, int columns)
  {
    usernameLabel = new Label(parent, SWT.NONE);
    usernameLabel.setText("User name:");

    usernameText = new Text(parent, SWT.BORDER);
    usernameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, columns - 1, 1));
    usernameText.addModifyListener(listener);
    usernameText.setFocus();

    passwordLabel = new Label(parent, SWT.NONE);
    passwordLabel.setText("Password:");

    passwordText = new Text(parent, SWT.BORDER | SWT.PASSWORD);
    passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, columns - 1, 1));
    passwordText.addModifyListener(listener);

    new Label(parent, SWT.NONE);

    signupLink = new Link(parent, SWT.NONE);
    signupLink.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, columns - 1, 1));
    signupLink.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        String uri = storage.getCreateAccountURI().toString();
        if (!SystemBrowser.open(uri))
        {
          String label = getServiceLabel(storage);
          MessageDialog.openInformation(getShell(), "System Browser Not Found", "Go to " + uri + " to create an " + label + " account.");
        }
      }
    });
  }

  protected void validate()
  {
  }

  private String getServiceLabel(IStorage storage)
  {
    String label = storage.getServiceLabel();
    if (label == null || label.length() == 0)
    {
      label = storage.getServiceURI().toString();
    }

    return label;
  }
}
