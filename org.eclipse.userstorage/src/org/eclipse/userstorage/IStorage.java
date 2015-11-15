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

import org.eclipse.userstorage.spi.StorageCache;
import org.eclipse.userstorage.spi.StorageFactory;
import org.eclipse.userstorage.util.BadKeyException;

/**
 * Represents a partition of the data in a {@link #getService() storage service} that
 * is associated with (i.e., created and maintained by) a {@link #getApplicationToken() registered application}.
 * <p>
 * A registered application gets access to its partition of the stored data by creating an instance of this interface as follows:
 * <p>
 * <pre>IStorage storage = StorageFactory.DEFAULT.create(applicationToken);<pre>
 * <p>
 * The <code>applicationToken</code> must be registered by the provider of the service of this storage.
 * The service provider is responsible for registering applications and providing the needed application token.
 * <p>
 * Once an application has created its storage the {@link #getBlob(String) getBlob()} method can be used to get access to
 * a specific piece of data that this application maintains for the logged-in user under a given <code>key</code>.
 * <p>
 *
 * @author Eike Stepper
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IStorage
{
  /**
   * Return the application token of this storage.
   * <p>
   * The <code>applicationToken</code> must be registered by the provider of the {@link #getService() service} of this storage.
   * The service provider is responsible for registering applications and providing the needed application token.
   * <p>
   *
   * @return the application token of this storage, never <code>null</code>.<p>
   *
   * @see StorageFactory#create(String)
   */
  public String getApplicationToken();

  /**
   * Returns the local storage cache of this storage.
   * <p>
   *
   * @return the local cache of this storage, or <code>null</code> if no cache was specified when this storage was created.<p>
   *
   * @see StorageFactory#create(String, StorageCache)
   */
  public StorageCache getCache();

  /**
   * Returns the service of this storage.
   * <p>
   *
   * @return the service of this storage, never <code>null</code>.<p>
   *
   * @see #setService(IStorageService)
   */
  public IStorageService getService();

  /**
   * Sets the service of this storage to the given service.
   * <p>
   * Calling this method is optional and only required if the default service that was assigned by the {@link StorageFactory storage factory} is not adequate.
   * <p>
   *
   * @param service the service to set into this storage, must not be <code>null</code>.<p>
   *
   * @see #getService()
   */
  public void setService(IStorageService service);

  /**
   * Provides access to a specific piece of data that this storage maintains for the logged-in user under the given <code>key</code>.
   * <p>
   * This method does not cause the remote service being contacted. It only performs minimal
   * {@link BadKeyException#validate(String) lexical validation} of the given <code>key</code>
   * and returns an {@link IBlob} instance that can be used to read or write the actual blob content.
   * <p>
   * This storage ensures that at no time two different IBlob instances for the same logged-in user and the same <code>key</code> exist in the same storage.
   * It also ensures that an IBlob instance becomes subject to garbage collection when the application releases its last strong reference on it.
   * <p>
   *
   * @param key the key that uniquely identifies this blob in the scope of the logged-in user and of this storage.
   *        Minimal {@link BadKeyException#validate(String) lexical validation} is performed on the passed key.<p>
   * @throws BadKeyException if {@link BadKeyException#validate(String) lexical validation} of the passed key fails.<p>
   * @returns the IBlob instance that represents the piece of data that this storage
   *          maintains for the logged-in user under the given <code>key</code>, never <code>null</code>.<p>
   */
  public IBlob getBlob(String key) throws BadKeyException;
}
