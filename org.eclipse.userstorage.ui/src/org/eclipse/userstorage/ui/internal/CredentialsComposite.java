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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import java.net.URI;
import java.util.concurrent.Callable;

/**
 * @author Eike Stepper
 */
public class CredentialsComposite extends Composite
{
  public static final Point INITIAL_SIZE = new Point(500, 350);

  private final Callable<URI> createAccountURIProvider = new Callable<URI>()
  {
    @Override
    public URI call() throws Exception
    {
      return service.getCreateAccountURI();
    }
  };

  private final Callable<URI> editAccountURIProvider = new Callable<URI>()
  {
    @Override
    public URI call() throws Exception
    {
      return service.getEditAccountURI();
    }
  };

  private final Callable<URI> recoverPasswordURIProvider = new Callable<URI>()
  {
    @Override
    public URI call() throws Exception
    {
      return service.getRecoverPasswordURI();
    }
  };

  private final ModifyListener modifyListener = new ModifyListener()
  {
    @Override
    public void modifyText(ModifyEvent e)
    {
      credentials = new Credentials(usernameText.getText(), passwordText.getText());
      validate();
    }
  };

  private final boolean showServiceCredentials;

  private IStorageService service;

  private Credentials credentials;

  private boolean termsOfUseAgreed;

  private Button termsOfUseButton;

  private MultiLink termsOfUseMultiLink;

  private Label spacer;

  private Label usernameLabel;

  private Text usernameText;

  private Label passwordLabel;

  private Text passwordText;

  private Link createAccountLink;

  private Link editAccountLink;

  private Link recoverPasswordLink;

  public CredentialsComposite(Composite parent, int style, int marginWidth, int marginHeight, boolean showServiceCredentials)
  {
    super(parent, style);
    this.showServiceCredentials = showServiceCredentials;

    GridLayout layout = UIUtil.createGridLayout(getGridColumns());
    layout.marginWidth = marginWidth;
    layout.marginHeight = marginHeight;
    setLayout(layout);

    createUI(this, layout.numColumns);
    setCredentials(null);
  }

  public IStorageService getService()
  {
    return service;
  }

  public void setService(IStorageService service)
  {
    this.service = service;
    if (service != null)
    {
      String termsOfUse = service.getTermsOfUseLink();
      if (StringUtil.isEmpty(termsOfUse))
      {
        hideTermsOfUse();
      }
      else
      {
        int columns = getGridColumns();

        termsOfUseButton.setVisible(true);
        termsOfUseButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

        termsOfUseMultiLink.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, columns - 1, 1));
        termsOfUseMultiLink.setVisible(true);
        termsOfUseMultiLink.setText(termsOfUse);

        spacer.setVisible(true);
        spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, columns, 1));
      }

      if (showServiceCredentials)
      {
        setCredentials(((StorageService)service).getCredentials());
        setTermsOfUseAgreed(((StorageService)service).isTermsOfUseAgreed());
      }
    }
    else
    {
      hideTermsOfUse();

      if (showServiceCredentials)
      {
        setCredentials(null);
        setTermsOfUseAgreed(false);
      }
    }

    updateEnablement();
    layout();
  }

  public boolean isTermsOfUseAgreed()
  {
    return termsOfUseAgreed;
  }

  public void setTermsOfUseAgreed(boolean termsOfUseAgreed)
  {
    this.termsOfUseAgreed = termsOfUseAgreed;
    termsOfUseButton.setSelection(termsOfUseAgreed);
    updateEnablement();
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
      usernameText.setText(StringUtil.EMPTY);
      passwordText.setText(StringUtil.EMPTY);
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
    createAccountLink.setEnabled(enabled);
    editAccountLink.setEnabled(enabled);
    recoverPasswordLink.setEnabled(enabled);
  }

  protected void createUI(Composite parent, int columns)
  {
    termsOfUseButton = new Button(parent, SWT.CHECK);
    termsOfUseButton.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        termsOfUseAgreed = termsOfUseButton.getSelection();
        updateEnablement();
        validate();
      }
    });

    termsOfUseMultiLink = new MultiLink.ForSystemBrowser(parent, SWT.WRAP);
    spacer = new Label(parent, SWT.NONE);
    hideTermsOfUse();

    usernameLabel = new Label(parent, SWT.NONE);
    usernameLabel.setText("User name:");

    usernameText = new Text(parent, SWT.BORDER);
    usernameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, columns - 1, 1));
    usernameText.addModifyListener(modifyListener);

    passwordLabel = new Label(parent, SWT.NONE);
    passwordLabel.setText("Password:");

    passwordText = new Text(parent, SWT.BORDER | SWT.PASSWORD);
    passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, columns - 1, 1));
    passwordText.addModifyListener(modifyListener);

    createAccountLink = createLink(parent, columns, "Create an account", createAccountURIProvider);
    editAccountLink = createLink(parent, columns, "Edit your account", editAccountURIProvider);
    recoverPasswordLink = createLink(parent, columns, "Recover your password", recoverPasswordURIProvider);
  }

  protected void validate()
  {
  }

  private Link createLink(Composite parent, int columns, final String label, final Callable<URI> uriProvider)
  {
    new Label(parent, SWT.NONE); // Skip first column.

    final Link link = new Link(parent, SWT.NONE);
    link.setText("<a>" + label + "</a>");
    link.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, columns - 1, 1));
    link.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        if (link.isEnabled())
        {
          try
          {
            String uri = uriProvider.call().toString();
            SystemBrowser.openSafe(getShell(), uri, "Go to " + uri + " to " + label.toLowerCase() + ".");
          }
          catch (Exception ex)
          {
            Activator.log(ex);
          }
        }
      }
    });

    return link;
  }

  private void enableLink(Link link, Callable<URI> uriProvider, boolean enabled)
  {
    try
    {
      link.setEnabled(enabled && uriProvider.call() != null);
    }
    catch (Exception ex)
    {
      //$FALL-THROUGH$
    }
  }

  private void updateEnablement()
  {
    boolean enabled = isValid();

    usernameLabel.setEnabled(enabled);
    usernameText.setEnabled(enabled);
    passwordLabel.setEnabled(enabled);
    passwordText.setEnabled(enabled);

    enableLink(createAccountLink, createAccountURIProvider, enabled);
    enableLink(editAccountLink, editAccountURIProvider, enabled);
    enableLink(recoverPasswordLink, recoverPasswordURIProvider, enabled);
  }

  private boolean isValid()
  {
    if (service == null)
    {
      return false;
    }

    String termsOfUseLink = service.getTermsOfUseLink();
    if (StringUtil.isEmpty(termsOfUseLink))
    {
      return true;
    }

    return termsOfUseAgreed;
  }

  private void hideTermsOfUse()
  {
    termsOfUseButton.setVisible(false);
    termsOfUseButton.setLayoutData(emptyGridData(1, 1));

    termsOfUseMultiLink.setVisible(false);
    termsOfUseMultiLink.setLayoutData(emptyGridData(getGridColumns() - 1, 1));
    termsOfUseMultiLink.setText(StringUtil.EMPTY);

    spacer.setVisible(false);
    spacer.setLayoutData(emptyGridData(getGridColumns(), 1));
  }

  private static GridData emptyGridData(int horizontalSpan, int verticalSpan)
  {
    GridData gridData = new GridData(0, 0);
    gridData.horizontalSpan = horizontalSpan;
    gridData.verticalSpan = verticalSpan;
    return gridData;
  }
}
