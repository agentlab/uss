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
import org.eclipse.userstorage.spi.Credentials;
import org.eclipse.userstorage.spi.ICredentialsProvider;
import org.eclipse.userstorage.ui.CredentialsDialog;

import org.eclipse.swt.widgets.Shell;

/**
 * @author Eike Stepper
 */
public final class DialogCredentialsProvider implements ICredentialsProvider
{
  public static final DialogCredentialsProvider INSTANCE = new DialogCredentialsProvider();

  @Override
  public Credentials provideCredentials(final IStorageService service)
  {
    final Credentials[] credentials = { null };

    try
    {
      final Shell shell = UIUtil.getShell();
      shell.getDisplay().syncExec(new Runnable()
      {
        @Override
        public void run()
        {
          CredentialsDialog dialog = new CredentialsDialog(shell, service);
          if (dialog.open() == CredentialsDialog.OK)
          {
            credentials[0] = dialog.getCredentials();
          }
        }
      });
    }
    catch (Throwable ex)
    {
      Activator.log(ex);
    }

    return credentials[0];
  }
}
