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
package org.eclipse.userstorage;

import org.eclipse.userstorage.internal.StorageSpace;
import org.eclipse.userstorage.spi.StorageCache;
import org.eclipse.userstorage.util.BadApplicationTokenException;
import org.eclipse.userstorage.util.BadKeyException;

import java.util.NoSuchElementException;

/**
 * Represents a partition of the data in a {@link IStorage storage} that
 * is associated with (i.e., created and maintained by) a {@link #getApplicationToken() registered application}.
 * <p>
 * A registered application gets access to its partition of the stored data by creating an instance of this interface as follows:
 * <p>
 * <pre>IStorageSpace storageSpace = IStorageSpace.Factory.create(applicationToken);<pre>
 * <p>
 * The <code>applicationToken</code> must be registered by the REST service behind the {@link #getStorage() storage} of this storage space.
 * The service provider is responsible for registering applications and providing the needed application token.
 * <p>
 * Once an application has created its storage space the {@link #getBlob(String) getBlob()} method can be used to get access to
 * a specific piece of data that this application maintains for the logged-in user under a given <code>key</code>.
 * <p>
 *
 * @author Eike Stepper
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IStorageSpace
{
  /**
   * Return the application token of this storage space.
   * <p>
   * The <code>applicationToken</code> must be registered by the REST service behind the {@link #getStorage() storage} of this storage space.
   * The service provider is responsible for registering applications and providing the needed application token.
   * <p>
   *
   * @return the application token of this storage space, never <code>null</code>.<p>
   *
   * @see IStorageSpace.Factory#create(String)
   */
  public String getApplicationToken();

  /**
   * Returns the local storage cache of this storage space.
   * <p>
   *
   * @return the local storage cache of this storage space, or <code>null</code> if no cache was specified when this storage space was created.<p>
   *
   * @see IStorageSpace.Factory#create(String, StorageCache)
   */
  public StorageCache getCache();

  /**
   * Returns the storage of this storage space.
   * <p>
   *
   * @return the storage of this storage space, never <code>null</code>.<p>
   *
   * @see #setStorage(IStorage)
   * @see IStorage.Registry#getDefaultStorage()
   */
  public IStorage getStorage();

  /**
   * Sets the storage of this storage space to the given storage.
   * <p>
   * Calling this method is optional and only required if the {@link IStorage.Registry#getDefaultStorage() default storage} is not adequate.
   * <p>
   *
   * @param storage the storage to set into this storage space, must not be <code>null</code>.<p>
   *
   * @see #getStorage()
   * @noreference This method is provisional and not intended to be referenced by clients.
   */
  public void setStorage(IStorage storage);

  /**
   * Provides access to a specific piece of data that this storage space maintains for the logged-in user under the given <code>key</code>.
   * <p>
   * This method does not cause the remote service being contacted. It only performs minimal
   * {@link BadKeyException#validate(String) lexical validation} of the given <code>key</code>
   * and returns an {@link IBlob} instance that can be used to read or write the actual blob content.
   * <p>
   * This storage space ensures that at no time two different IBlob instances for the same logged-in user and the same <code>key</code> exist in the same storage space.
   * It also ensures that an IBlob instance becomes subject to garbage collection when the application releases its last strong reference on it.
   * <p>
   *
   * @param key the key that uniquely identifies this blob in the scope of the logged-in user and of this storage space.
   *        Minimal {@link BadKeyException#validate(String) lexical validation} is performed on the passed key.<p>
   * @throws BadKeyException if {@link BadKeyException#validate(String) lexical validation} of the passed key fails.<p>
   * @returns the IBlob instance that represents the piece of data that this storage space
   *          maintains for the logged-in user under the given <code>key</code>, never <code>null</code>.<p>
   */
  public IBlob getBlob(String key) throws BadKeyException;

  /**
   * Supports the creation of {@link IStorageSpace storage spaces}.
   *
   * @author Eike Stepper
   */
  public static final class Factory
  {
    /**
     * Creates a storage space for application identified by the given <code>application token</code>.
     * <p>
     * Calling this method is identical to calling <code>create(applicationToken, null)</code>.
     * <p>
     *
     * @param applicationToken the application token that identifies the application of the storage space to be created.
     *        Minimal {@link BadApplicationTokenException#validate(String) lexical validation} is performed on the passed application token.<p>
     * @return the newly created storage space, never <code>null</code>.<p>
     * @throws NoSuchElementException if the {@link IStorage.Registry storage registry} is empty and, hence, there is no default storage available.<p>
     * @throws BadApplicationTokenException if {@link BadApplicationTokenException#validate(String) lexical validation} of the passed application token fails.<p>
     *
     * @see #create(String, StorageCache)
     */
    public static IStorageSpace create(String applicationToken) throws NoSuchElementException, BadApplicationTokenException
    {
      return create(applicationToken, null);
    }

    /**
     * Creates a storage space for application identified by the given <code>application token</code> and associates it with a given {@link StorageCache storage cache}.
     * <p>
     * @param applicationToken the application token that identifies the application of the storage space to be created.
     *        Minimal {@link BadApplicationTokenException#validate(String) lexical validation} is performed on the passed application token.<p>
     * @param cache a local storage cache to be used as a locally persistent optimization, or <code>null</code> if local caching is not wanted.<p>
     * @return the newly created storage space, never <code>null</code>.<p>
     * @throws NoSuchElementException if the {@link IStorage.Registry storage registry} is empty and, hence, there is no default storage available.<p>
     * @throws BadApplicationTokenException if {@link BadApplicationTokenException#validate(String) lexical validation} of the passed application token fails.<p>
     *
     * @see #create(String)
     * @see StorageCache
     */
    public static IStorageSpace create(String applicationToken, StorageCache cache) throws NoSuchElementException, BadApplicationTokenException
    {
      return StorageSpace.create(applicationToken, cache);
    }
  }
}
