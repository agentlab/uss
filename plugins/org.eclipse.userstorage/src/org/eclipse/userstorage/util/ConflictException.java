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

import java.io.Serializable;
import java.net.URI;

/**
 * Signals that an HTTP conflict exception has occurred (409 Conflict).
 *
 * @author Eike Stepper
 */
public class ConflictException extends ProtocolException
{
  private static final long serialVersionUID = 1L;

  private String eTag;

  /**
   * Public constructor to make this exception {@link Serializable}.
   */
  public ConflictException()
  {
  }

  /**
   * Constructs this exception with the given parameters.
   *
   * @param method the HTTP method of the request that causes this exception.
   * @param uri the URI of the request that causes this exception.
   * @param protocolVersion the HTTP protocol version of the request that causes this exception.
   * @param reasonPhrase the HTTP status reason phrase of the response that causes this exception.
   * @param eTag the HTTP ETag of the response that causes this exception.
   */
  public ConflictException(String method, URI uri, String protocolVersion, String reasonPhrase, String eTag)
  {
    super(method, uri, protocolVersion, 409, reasonPhrase);
    this.eTag = eTag;
  }

  /**
   * Returns the HTTP ETag of the request that caused this exception.
   *
   * @return the HTTP ETag of the request that caused this exception.
   */
  public final String getETag()
  {
    return eTag;
  }
}
