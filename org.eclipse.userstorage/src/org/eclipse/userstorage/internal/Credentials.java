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

import org.eclipse.userstorage.internal.util.StringUtil;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Eike Stepper
 */
public final class Credentials implements Serializable
{
  private static final long serialVersionUID = 1L;

  private String username;

  private byte[] password;

  public Credentials()
  {
  }

  public Credentials(String username, String password)
  {
    this.username = username;
    this.password = StringUtil.encrypt(password);
  }

  public String getUsername()
  {
    return username;
  }

  public String getPassword()
  {
    return StringUtil.decrypt(password);
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(password);
    result = prime * result + (username == null ? 0 : username.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }

    if (obj == null)
    {
      return false;
    }

    if (getClass() != obj.getClass())
    {
      return false;
    }

    Credentials other = (Credentials)obj;
    if (!Arrays.equals(password, other.password))
    {
      return false;
    }

    if (username == null)
    {
      if (other.username != null)
      {
        return false;
      }
    }
    else if (!username.equals(other.username))
    {
      return false;
    }

    return true;
  }

  @Override
  public String toString()
  {
    return username;
  }
}
