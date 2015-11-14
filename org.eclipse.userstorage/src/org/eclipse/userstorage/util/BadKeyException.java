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

import org.eclipse.userstorage.IBlob;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Signals that the lexical conventions for {@link IBlob#getKey() keys} have been violated.
 * <p>
 * The lexical conventions for keys are:
 * <p>
 * <ul>
 * <li> 5-25 characters are required.
 * <li> Lower case letters, upper case letters, digits, and underscores are valid characters.
 * <li> First character must be a lower case or upper case letter.
 * </ul>
 * <p>
 *
 * @author Eike Stepper
 */
public final class BadKeyException extends RuntimeException
{
  private static final int MIN_CHARS = 5;

  private static final int MAX_CHARS = 25;

  private static final Pattern PATTERN = Pattern.compile("[a-zA-Z][a-zA-z0-9_]*");

  private static final long serialVersionUID = 1L;

  /**
   * Public constructor to make this exception {@link Serializable}.
   */
  public BadKeyException()
  {
  }

  private BadKeyException(String message)
  {
    super(message);
  }

  /**
   * Validates that the given {@link IBlob#getKey() key} complies
   * with the {@link BadKeyException lexical conventions} for keys.
   * <p>
   *
   * @param key the key to validate.<p>
   * @return the passed key if it complies with the lexical conventions, i.e.,
   *         if no BadKeyException is thrown.<p>
   * @throws BadKeyException if the passed key violates the lexical conventions.
   */
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
