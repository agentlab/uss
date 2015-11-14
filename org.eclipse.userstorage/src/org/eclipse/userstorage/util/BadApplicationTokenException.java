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

import org.eclipse.userstorage.IStorageSpace;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Signals that the lexical conventions for {@link IStorageSpace#getApplicationToken() application tokens} have been violated.
 * <p>
 * The lexical conventions for application tokens are:
 * <p>
 * <ul>
 * <li> 5-27 characters are required.
 * <li> Lower case letters, upper case letters, digits, and underscores are valid characters.
 * <li> First character must be a lower case or upper case letter.
 * </ul>
 * <p>
 *
 * @author Eike Stepper
 */
public final class BadApplicationTokenException extends RuntimeException
{
  private static final int MIN_CHARS = 5;

  private static final int MAX_CHARS = 27;

  private static final Pattern PATTERN = Pattern.compile("[a-zA-Z][a-zA-z0-9_]*");

  private static final long serialVersionUID = 1L;

  /**
   * Public constructor to make this exception {@link Serializable}.
   */
  public BadApplicationTokenException()
  {
  }

  private BadApplicationTokenException(String message)
  {
    super(message);
  }

  /**
   * Validates that the given {@link IStorageSpace#getApplicationToken() application token} complies
   * with the {@link BadApplicationTokenException lexical conventions} for application tokens.
   * <p>
   *
   * @param applicationToken the application token to validate.<p>
   * @return the passed application token if it complies with the lexical conventions, i.e.,
   *         if no BadApplicationTokenException is thrown.<p>
   * @throws BadApplicationTokenException if the passed application token violates the lexical conventions.
   */
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
