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
package org.eclipse.userstorage.spi;

import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.internal.Activator;
import org.eclipse.userstorage.internal.Credentials;

/**
 * Provides the user's credentials for a given {@link IStorageService storage service}.
 *
 * @author Eike Stepper
 */
public interface ICredentialsProvider
{
  /**
   * A credentials provider that returns no credentials and, hence, simulates authentication cancelation.
   */
  public static final ICredentialsProvider CANCEL = Activator.CANCEL_CREDENTIALS_PROVIDER;

  /**
   * Provides the user's credentials for the given {@link IStorageService storage service}.
   * <p>
   *
   * @param service the storage service for which to provide the user's credentials, must not be <code>null</code>.<p>
   * @return the user's credentials for the given storage service,
   *         or <code>null</code> as an indication to cancel the authentication process.<p>
   */
  public Credentials provideCredentials(IStorageService service);
}
