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

import org.eclipse.userstorage.internal.StorageRegistry;

import java.net.URI;
import java.util.NoSuchElementException;

/**
 * Represents a remote <i>user storage service</i> (USS).
 * <p>
 * The {@link Registry storage registry} makes known storages available and supports the
 * {@link Registry#addStorage(String, URI, URI, URI, URI) addition} of {@link Dynamic dynamic}
 * storages.
 * <p>
 *
 * @author Eike Stepper
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IStorage
{
  /**
   * Returns the label of the REST service behind this storage.
   *
   * @return the label of the REST service behind this storage, never <code>null</code>.<p>
   */
  public String getServiceLabel();

  /**
   * Returns the URI of the REST service behind this storage.
   * <p>
   * The service URI is a unique identifier of a storage and it is used as the lookup key in
   * {@link Registry#getStorage(URI) Registry.getStorage()}.
   *
   * @return the URI of the REST service behind this storage, never <code>null</code>.<p>
   */
  public URI getServiceURI();

  /**
   * Returns the create account URI of this storage.
   *
   * @return the create account URI of this storage, can be <code>null</code>.<p>
   */
  public URI getCreateAccountURI();

  /**
   * Returns the edit account URI of this storage.
   *
   * @return the edit account URI of this storage, can be <code>null</code>.<p>
   */
  public URI getEditAccountURI();

  /**
   * Returns the recover password URI of this storage.
   *
   * @return the recover password URI of this storage, can be <code>null</code>.<p>
   */
  public URI getRecoverPasswordURI();

  /**
   * A {@link IStorage storage} that is dynamically created as opposed to being
   * statically contributed via the <code>org.eclipse.userstorage.storages</code> extension point).
   * <p>
   * Dynamic storages can be created and registered via the {@link Registry#addStorage(String, URI, URI, URI, URI) addStorage()} method
   * and only dynamic storages can be {@link #remove() removed} from the {@link Registry storage registry}.
   * <p>
   *
   * @author Eike Stepper
   * @noextend This interface is not intended to be extended by clients.
   * @noimplement This interface is not intended to be implemented by clients.
   */
  public interface Dynamic extends IStorage
  {
    /**
     * Removes this storage from the {@link Registry storage registry}.
     */
    public void remove();
  }

  /**
   * Provides a central place to access registered {@link IStorage storages} and
   * maintains a {@link #getDefaultStorage() default} storage.
   * <p>
   * This registry contains storages of the following two types:
   * <p>
   * <ul>
   * <li> Static storages that are contributed via the <code>org.eclipse.userstorage.storages</code> extension point.
   * <li> {@link Dynamic Dynamic} storages that are created via the {@link #addStorage(String, URI, URI, URI, URI) addStorage()} method.
   * </ul>
   * <p>
   * To access the storages in this registry an application uses the {@link #INSTANCE} constant as follows:
   * <p>
   * <pre>   IStorage[] storages = IStorage.Registry.INSTANCE.getStorages();</pre>
   * <p>
   * To access the data that is stored in a storage on the server side an application must create a {@link IStorageSpace storage space}.
   * <p>
   *
   * @author Eike Stepper
   * @noextend This interface is not intended to be extended by clients.
   * @noimplement This interface is not intended to be implemented by clients.
   */
  public interface Registry
  {
    /**
     * The singleton instance of this registry.
     */
    public static final Registry INSTANCE = StorageRegistry.INSTANCE;

    /**
     * Returns an array of all currently registered storages.
     *
     * @return an array of all currently registered storages, never <code>null</code>.<p>
     */
    public IStorage[] getStorages();

    /**
     * Returns the currently registered storage for the given <code>serviceURI</code>.
     *
     * @return the currently registered storage for the given <code>serviceURI</code>,
     *         or <code>null</code> if no storage is registered for the given <code>serviceURI</code>.<p>
     */
    public IStorage getStorage(URI serviceURI);

    /**
     * Returns the current default storage.
     * <p>
     * The current default storage can change if any of the following happens:
     * <p>
     * <ul>
     * <li>If the {@link #setDefaultStorage(IStorage) setDefaultStorage()} of this registry is called with a different storage.
     * <li>If the current default storage is {@link Dynamic dynamic} and {@link Dynamic#remove() remove()} is called on it.
     * </ul>
     * <p>
     * Newly created {@link IStorageSpace storage spaces} are initially {@link IStorageSpace#getStorage() associated}
     * with the default storage of this registry. Changing the default storage of this registry has no effect on already created storage spaces.
     * <p>
     *
     * @return the current default storage, never <code>null</code>.<p>
     * @throws NoSuchElementException if this registry is empty and, hence, there is no default storage available.<p>
     *
     * @see #setDefaultStorage(IStorage)
     * @see IStorageSpace#setStorage(IStorage)
     */
    public IStorage getDefaultStorage() throws NoSuchElementException;

    /**
     * Sets the default storage to the given <code>defaultStorage</code>.
     * <p>
     * Newly created {@link IStorageSpace storage spaces} are initially {@link IStorageSpace#getStorage() associated}
     * with the default storage of this registry. Changing the default storage of this registry has no effect on already created storage spaces.
     * <p>
     *
     * @see #getDefaultStorage()
     * @see IStorageSpace#setStorage(IStorage)
     */
    public void setDefaultStorage(IStorage defaultStorage);

    /**
     * Adds a new dynamic storage with the given <code>serviceLabel</code> and the given <code>serviceURI</code> to this registry.
     * <p>
     *
     * @param serviceLabel the label of the REST service behind the storage to be created and registered.
     *        The label <i>must not be</i> <code>null</code> or empty and it <i>should be</i> unique (the latter is not a strict requirement
     *        but rather a recommendation to make it easier for a user to pick a storage for an application). See also {@link IStorage#getServiceLabel()}.<p>
     * @param serviceURI the base URI of the REST service behind the storage to be created and registered.
     *        The service URI is a unique identifier of the storage and it is used as the lookup key in
     *        {@link Registry#getStorage(URI) Registry.getStorage()}. It <i>must not be</i> <code>null</code>. See also {@link IStorage#getServiceURI()}.<p>
     * @param createAccountURI an optional (<i>can be</i> <code>null</code>) URI that a user interface can use to point the user to a web page that supports the creation
     *        of the user account needed for the REST service behind the storage to be created and registered. See also {@link IStorage#getCreateAccountURI()}.<p>
     * @param editAccountURI an optional (<i>can be</i> <code>null</code>) URI that a user interface can use to point the user to a web page that supports the modification
     *        of the user account needed for the REST service behind the storage to be created and registered. See also {@link IStorage#getEditAccountURI()}.<p>
     * @param recoverPasswordURI an optional (<i>can be</i> <code>null</code>) URI that a user interface can use to point the user to a web page that supports the recovery
     *        of the password needed to log into the REST service behind the storage to be created and registered. See also {@link IStorage#getRecoverPasswordURI()}.<p>
     *
     * @return the newly created and registered storage, never <code>null</code>.<p>
     * @throws IllegalStateException if a storage with the same <code>serviceURI</code> is already registered in this registry.<p>
     *
     * @see #addStorage(String, URI)
     */
    public IStorage.Dynamic addStorage(String serviceLabel, URI serviceURI, URI createAccountURI, URI editAccountURI, URI recoverPasswordURI)
        throws IllegalStateException;

    /**
     * Adds a new dynamic storage with the given <code>serviceLabel</code> and the given <code>serviceURI</code> to this registry.
     * <p>
     * Calling this method is identical to calling <code>addStorage(serviceLabel, serviceURI, null, null, null)</code>.
     * <p>
     *
     * @param serviceLabel the label of the REST service behind the storage to be created and registered.
     *        The label <i>must not be</i> <code>null</code> or empty and it <i>should be</i> unique (the latter is not a strict requirement
     *        but rather a recommendation to make it easier for a user to pick a storage for an application). See also {@link IStorage#getServiceLabel()}.<p>
     * @param serviceURI the base URI of the REST service behind the storage to be created and registered.
     *        The service URI is a unique identifier of the storage and it is used as the lookup key in
     *        {@link Registry#getStorage(URI) Registry.getStorage()}. It <i>must not be</i> <code>null</code>. See also {@link IStorage#getServiceURI()}.<p>
     *
     * @return the newly created and registered storage, never <code>null</code>.<p>
     * @throws IllegalStateException if a storage with the same <code>serviceURI</code> is already registered in this registry.<p>
     *
     * @see #addStorage(String, URI, URI, URI, URI)
     */
    public IStorage.Dynamic addStorage(String serviceLabel, URI serviceURI) throws IllegalStateException;
  }
}
