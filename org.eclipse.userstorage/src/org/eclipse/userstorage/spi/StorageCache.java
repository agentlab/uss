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

import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.internal.InternalStorageCache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * Caches the {@link IBlob#getProperties() properties} and {@link IBlob#getContents() contents}
 * of {@link IBlob blobs} locally.
 * <p>
 *
 * @author Eike Stepper
 */
public abstract class StorageCache extends InternalStorageCache
{
  /**
   * Constructs this cache.
   */
  public StorageCache()
  {
    // Do nothing.
  }

  /**
   * Returns the storage of this cache.
   *
   * @return the storage of this cache, never <code>null</code>.<p>
   *
   * @see IStorage#setService(IStorageService)
   */
  @Override
  public final IStorageService getService()
  {
    return super.getService();
  }

  /**
   * This method is called when an application has called {@link IStorage#setService(IStorageService) setStorage()}
   * on a {@link IStorage storage} that was created with a cache.
   * <p>
   *
   * @param oldService the old service of this cache.<p>
   * @param newService the new service of this cache.<p>
   *
   * @see IStorage#setService(IStorageService)
   */
  @Override
  protected void storageChanged(IStorageService oldService, IStorageService newService)
  {
    // Do nothing.
  }

  /**
   * Returns an {@link Iterator} over the {@link IBlob#getKey() keys} of all blobs with
   * the given {@link IStorage#getApplicationToken() application token} that are cached in this cache.
   * <p>
   *
   * @param applicationToken the {@link IStorage#getApplicationToken() application token} for which to return all keys,
   *        must not be <code>null</code>.<p>
   * @return an {@link Iterator} over the {@link IBlob#getKey() keys} of all blobs with
   *         the given applicationToken that are cached in this cache, never <code>null</code>.<p>
   * @throws IOException if local I/O was unsuccessful.<p>
   */
  @Override
  public abstract Iterator<String> getKeys(String applicationToken) throws IOException;

  /**
   * Loads the properties of the blob with the given {@link IStorage#getApplicationToken() application token}
   * and {@link IBlob#getKey() key} from this cache into the given properties map.
   * <p>
   *
   * @param applicationToken the {@link IStorage#getApplicationToken() application token} for which to load the properties,
   *        must not be <code>null</code>.<p>
   * @param key the {@link IBlob#getKey() key} for which to load the properties,
   *        must not be <code>null</code>.<p>
   * @param properties the properties map into which to load the properties,
   *        must not be <code>null</code>.<p>
   * @throws IOException if local I/O was unsuccessful.<p>
   */
  @Override
  protected abstract void loadProperties(String applicationToken, String key, Map<String, String> properties) throws IOException;

  /**
   * Saves the given properties of the blob with the given {@link IStorage#getApplicationToken() application token}
   * and {@link IBlob#getKey() key} into this cache.
   * <p>
   *
   * @param applicationToken the {@link IStorage#getApplicationToken() application token} for which to save the properties,
   *        must not be <code>null</code>.<p>
   * @param key the {@link IBlob#getKey() key} for which to save the properties,
   *        must not be <code>null</code>.<p>
   * @param properties the properties map to save,
   *        must not be <code>null</code>.<p>
   * @throws IOException if local I/O was unsuccessful.<p>
   */
  @Override
  protected abstract void saveProperties(String applicationToken, String key, Map<String, String> properties) throws IOException;

  /**
   * Returns an {@link InputStream} that represents the cached contents of the blob with the given
   * {@link IStorage#getApplicationToken() application token} and {@link IBlob#getKey() key}.
   * <p>
   *
   * @param applicationToken the {@link IStorage#getApplicationToken() application token} for which to return the contents stream,
   *        must not be <code>null</code>.<p>
   * @param key the {@link IBlob#getKey() key} for which to return the contents stream,
   *        must not be <code>null</code>.<p>
   * @return an {@link InputStream} that represents the cached contents of this blob, never <code>null</code>.<p>
   * @throws IOException if local I/O was unsuccessful, including the case that the contents disappeared from this cache.<p>
   */
  @Override
  protected abstract InputStream getInputStream(String applicationToken, String key) throws IOException;

  /**
   * Returns an {@link OutputStream} that represents the cached contents of the blob with the given
   * {@link IStorage#getApplicationToken() application token} and {@link IBlob#getKey() key}.
   * <p>
   *
   * @param applicationToken the {@link IStorage#getApplicationToken() application token} for which to return the contents stream,
   *        must not be <code>null</code>.<p>
   * @param key the {@link IBlob#getKey() key} for which to return the contents stream,
   *        must not be <code>null</code>.<p>
   * @return an {@link OutputStream} that represents the cached contents of this blob, never <code>null</code>.<p>
   * @throws IOException if local I/O was unsuccessful.<p>
   */
  @Override
  protected abstract OutputStream getOutputStream(String applicationToken, String key) throws IOException;

  /**
   * Deletes the blob with the given {@link IStorage#getApplicationToken() application token} and {@link IBlob#getKey() key}
   * from this cache.
   * <p>
   *
   * @param applicationToken the {@link IStorage#getApplicationToken() application token} of the blob to delete,
   *        must not be <code>null</code>.<p>
   * @param key the {@link IBlob#getKey() key} of the blob to delete,
   *        must not be <code>null</code>.<p>
   * @throws IOException if local I/O was unsuccessful.<p>
   */
  @Override
  protected abstract void delete(String applicationToken, String key) throws IOException;
}
