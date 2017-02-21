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

import org.eclipse.userstorage.internal.util.StringUtil;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;

/**
 * Signals that an HTTP protocol exception of some sort has occurred.
 *
 * @author Eike Stepper
 * @see ConflictException
 */
public class ProtocolException extends IOException
{
  private static final long serialVersionUID = 1L;

  private String method;

  private URI uri;

  private String protocolVersion;

  private int statusCode;

  private String reasonPhrase;

  /**
   * Public constructor to make this exception {@link Serializable}.
   */
  public ProtocolException()
  {
  }

  /**
   * Constructs this exception with the given parameters.
   *
   * @param method the HTTP method of the request that causes this exception.
   * @param uri the URI of the request that causes this exception.
   * @param protocolVersion the HTTP protocol version of the request that causes this exception.
   * @param statusCode the HTTP status code of the response that causes this exception.
   * @param reasonPhrase the HTTP status reason phrase of the response that causes this exception.
   */
  public ProtocolException(String method, URI uri, String protocolVersion, int statusCode, String reasonPhrase)
  {
    super(method + " " + uri + " " + protocolVersion + " " + statusCode + (StringUtil.isEmpty(reasonPhrase) ? "" : " " + reasonPhrase));
    this.method = method;
    this.uri = uri;
    this.protocolVersion = protocolVersion;
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
  }

  /**
   * Returns the HTTP method of the request that caused this exception.
   *
   * @return the HTTP method of the request that caused this exception.
   */
  public final String getMethod()
  {
    return method;
  }

  /**
   * Returns the URI of the request that caused this exception.
   *
   * @return the URI of the request that caused this exception.
   */
  public final URI getURI()
  {
    return uri;
  }

  /**
   * Returns the HTTP protocol version of the request that caused this exception.
   *
   * @return the HTTP protocol version of the request that caused this exception.
   */
  public final String getProtocolVersion()
  {
    return protocolVersion;
  }

  /**
   * Returns the HTTP status code of the response that caused this exception.
   *
   * @return the HTTP status code of the response that caused this exception.
   */
  public final int getStatusCode()
  {
    return statusCode;
  }

  /**
   * Returns the HTTP status reason phrase of the response that caused this exception.
   *
   * @return the HTTP status reason phrase of the response that caused this exception.
   */
  public final String getReasonPhrase()
  {
    return reasonPhrase;
  }
}
