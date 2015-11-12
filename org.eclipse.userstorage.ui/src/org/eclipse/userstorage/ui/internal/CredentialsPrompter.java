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

import org.eclipse.swt.widgets.Shell;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.internal.Credentials;
import org.eclipse.userstorage.spi.ICredentialsProvider;

/**
 * @author Eike Stepper
 */
public final class CredentialsPrompter implements ICredentialsProvider
{
  public static final CredentialsPrompter INSTANCE = new CredentialsPrompter();

  @Override
  public Credentials provideCredentials(final IStorage storage)
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
          CredentialsDialog dialog = new CredentialsDialog(shell, storage);
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
