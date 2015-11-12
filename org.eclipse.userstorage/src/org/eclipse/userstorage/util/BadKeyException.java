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
package org.eclipse.userstorage.util;

import java.util.regex.Pattern;

/**
 * @author Eike Stepper
 */
public final class BadKeyException extends RuntimeException
{
  private static final int MIN_CHARS = 5;

  private static final int MAX_CHARS = 25;

  private static final Pattern PATTERN = Pattern.compile("[a-zA-Z][a-zA-z0-9_]*");

  private static final long serialVersionUID = 1L;

  public BadKeyException()
  {
  }

  private BadKeyException(String message)
  {
    super(message);
  }

  public static String validate(String key) throws BadKeyException
  {
    if (key == null)
    {
      throw new BadKeyException("Key is null");
    }

    int length = key.length();
    if (length < MIN_CHARS)
    {
      throw new BadKeyException("Key is shorter than the minimum of " + MIN_CHARS + " characters");
    }

    if (length > MAX_CHARS)
    {
      throw new BadKeyException("Key is longer than the maximum of " + MAX_CHARS + " characters");
    }

    if (!PATTERN.matcher(key).matches())
    {
      throw new BadKeyException("Key '" + key + "' does not match pattern " + PATTERN);
    }

    return key;
  }
}
