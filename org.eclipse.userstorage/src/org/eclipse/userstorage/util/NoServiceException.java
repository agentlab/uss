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

import org.eclipse.userstorage.IStorage;

import java.io.Serializable;

/**
 * Signals that a {@link IStorage storage} is used, but no {@link IStorage#getService() service} could be assigned.
 *
 * @author Eike Stepper
 */
public class NoServiceException extends IllegalStateException
{
  private static final long serialVersionUID = 1L;

  /**
   * Public constructor to make this exception {@link Serializable}.
   */
  public NoServiceException()
  {
  }
}
