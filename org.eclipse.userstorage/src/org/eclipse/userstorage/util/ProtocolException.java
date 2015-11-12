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
import java.net.URI;

/**
 * @author Eike Stepper
 */
public class ProtocolException extends IOException
{
  private static final long serialVersionUID = 1L;

  private String method;

  private URI uri;

  private String protocolVersion;

  private int statusCode;

  private String reasonPhrase;

  public ProtocolException()
  {
  }

  public ProtocolException(String method, URI uri, String protocolVersion, int statusCode, String reasonPhrase)
  {
    super(method + " " + uri + " " + protocolVersion + " " + statusCode + (StringUtil.isEmpty(reasonPhrase) ? "" : " " + reasonPhrase));
    this.method = method;
    this.uri = uri;
    this.protocolVersion = protocolVersion;
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
  }

  public final String getMethod()
  {
    return method;
  }

  public final URI getURI()
  {
    return uri;
  }

  public final String getProtocolVersion()
  {
    return protocolVersion;
  }

  public final int getStatusCode()
  {
    return statusCode;
  }

  public final String getReasonPhrase()
  {
    return reasonPhrase;
  }
}
