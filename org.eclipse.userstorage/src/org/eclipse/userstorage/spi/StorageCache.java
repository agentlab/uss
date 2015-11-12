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
package org.eclipse.userstorage.spi;

import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.internal.InternalStorageCache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public abstract class StorageCache extends InternalStorageCache
{
  public StorageCache()
  {
  }

  @Override
  public final IStorage getStorage()
  {
    return super.getStorage();
  }

  /**
   * Subclasses may override.
   */
  @Override
  protected void storageChanged(IStorage oldStorage, IStorage newStorage)
  {
    // Do nothing.
  }

  @Override
  public abstract Iterator<String> getKeys(String applicationToken) throws IOException;

  @Override
  protected abstract void loadProperties(String applicationToken, String key, Map<String, String> properties) throws IOException;

  @Override
  protected abstract void saveProperties(String applicationToken, String key, Map<String, String> properties) throws IOException;

  @Override
  protected abstract InputStream getInputStream(String applicationToken, String key) throws IOException;

  @Override
  protected abstract OutputStream getOutputStream(String applicationToken, String key) throws IOException;

  @Override
  protected abstract void delete(String applicationToken, String key) throws IOException;
}
