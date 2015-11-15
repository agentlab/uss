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
package org.eclipse.userstorage.internal;

/**
 * @author Eike Stepper
 */
public final class StorageProperties
{
  public static final String SETTINGS = "org.eclipse.userstorage.settings";

  public static final String CREDENTIALS_PROVIDER = "org.eclipse.userstorage.credentialsProvider";

  public static final String CONNECT_TIMEOUT = "org.eclipse.userstorage.connectTimeout";

  public static final String SOCKET_TIMEOUT = "org.eclipse.userstorage.socketTimeout";

  private StorageProperties()
  {
  }

  public static int getProperty(String key, int defaultValue)
  {
    try
    {
      String property = System.getProperty(key);
      return Integer.parseInt(property);
    }
    catch (Exception ex)
    {
      return defaultValue;
    }
  }
}
