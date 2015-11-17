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
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.StorageFactory;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.util.FileStorageCache;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Eike Stepper
 */
public class Example
{
  public static void main(String[] args) throws IOException
  {
    IStorage storage = StorageFactory.DEFAULT.create("pDKTqBfDuNxlAKydhEwxBZPxa4q", new FileStorageCache());

    IBlob blob = storage.getBlob("user_setup");
    blob.setContentsUTF("A short UTF-8 string value");

    InputStream in = blob.getContents();
    IOUtil.copy(in, new FileOutputStream("user.txt"));
    IOUtil.close(in);
  }
}
