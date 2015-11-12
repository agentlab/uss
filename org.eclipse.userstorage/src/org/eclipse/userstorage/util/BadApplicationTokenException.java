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
public final class BadApplicationTokenException extends RuntimeException
{
  private static final int MIN_CHARS = 5;

  private static final int MAX_CHARS = 27;

  private static final Pattern PATTERN = Pattern.compile("[a-zA-Z][a-zA-z0-9_]*");

  private static final long serialVersionUID = 1L;

  public BadApplicationTokenException()
  {
  }

  private BadApplicationTokenException(String message)
  {
    super(message);
  }

  public static String validate(String applicationToken) throws BadApplicationTokenException
  {
    if (applicationToken == null)
    {
      throw new BadApplicationTokenException("Application token is null");
    }

    int length = applicationToken.length();
    if (length < MIN_CHARS)
    {
      throw new BadApplicationTokenException("Application token is shorter than the minimum of " + MIN_CHARS + " characters");
    }

    if (length > MAX_CHARS)
    {
      throw new BadApplicationTokenException("Application token is longer than the maximum of " + MAX_CHARS + " characters");
    }

    if (!PATTERN.matcher(applicationToken).matches())
    {
      throw new BadApplicationTokenException("Application token '" + applicationToken + "' does not match pattern " + PATTERN);
    }

    return applicationToken;
  }
}
