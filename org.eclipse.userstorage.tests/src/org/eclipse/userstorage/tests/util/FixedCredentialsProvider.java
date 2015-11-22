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
package org.eclipse.userstorage.tests.util;

import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.internal.Credentials;
import org.eclipse.userstorage.spi.ICredentialsProvider;

/**
 * @author Eike Stepper
 */
public class FixedCredentialsProvider implements ICredentialsProvider
{
  public static final Credentials DEFAULT_CREDENTIALS = new Credentials("eclipse_test_123456789", "plaintext123456789");

  private static Credentials credentials = DEFAULT_CREDENTIALS;

  public FixedCredentialsProvider()
  {
  }

  @Override
  public Credentials provideCredentials(IStorageService service)
  {
    return credentials;
  }

  public static Credentials setCredentials(Credentials credentials)
  {
    Credentials oldCredentials = FixedCredentialsProvider.credentials;
    FixedCredentialsProvider.credentials = credentials;
    return oldCredentials;
  }
}
