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

import java.net.URI;

/**
 * @author Eike Stepper
 */
public class ConflictException extends ProtocolException
{
  private static final long serialVersionUID = 1L;

  private String eTag;

  public ConflictException()
  {
  }

  public ConflictException(String method, URI uri, String protocolVersion, int statusCode, String reasonPhrase, String eTag)
  {
    super(method, uri, protocolVersion, statusCode, reasonPhrase);
    this.eTag = eTag;
  }

  public final String getETag()
  {
    return eTag;
  }
}
