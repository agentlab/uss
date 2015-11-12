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
package org.eclipse.userstorage.tests;

import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.IStorageSpace;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.util.FileStorageCache;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Eike Stepper
 */
public class Examples
{
  public static void main(String[] args) throws IOException
  {
    IStorageSpace storageSpace = IStorageSpace.Factory.create("cNhDr0INs8T109P8h6E1r_GvU3I", new FileStorageCache());

    IBlob blob = storageSpace.getBlob("user_setup");
    InputStream in = blob.getContents();
    copy(in, new FileOutputStream("user.xml"));
    IOUtil.close(in);

    blob.setContentsUTF("A short UTF-8 string value");

  }

  private static void copy(InputStream in, OutputStream out)
  {
  }
}
