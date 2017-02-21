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

import org.eclipse.userstorage.IStorageService.Registry;
import org.eclipse.userstorage.internal.Storage;
import org.eclipse.userstorage.spi.ISettings;
import org.eclipse.userstorage.spi.StorageCache;
import org.eclipse.userstorage.util.BadApplicationTokenException;
import org.eclipse.userstorage.util.Settings;
import org.eclipse.userstorage.util.Settings.MemorySettings;

/**
 * Creates {@link IStorage storages} and maintains their preferred {@link IStorage#getService() services}
 * in the supplied {@link #getSettings() settings}.
 *
 * @author Eike Stepper
 */
public final class StorageFactory
{
  public static final StorageFactory DEFAULT = new StorageFactory(Settings.DEFAULT);

  private final ISettings settings;

  /**
   * Constructs this storage factory with the given settings.
   *
   * @param settings the settings to use with this storage factory, or <code>null</code> for {@link Settings#NONE no settings}.
   */
  public StorageFactory(ISettings settings)
  {
    this.settings = settings != null ? settings : Settings.NONE;
  }

  /**
   * Constructs this storage factory with {@link MemorySettings in-memory settings}.
   */
  public StorageFactory()
  {
    this(new MemorySettings());
  }

  /**
   * Returns the settings of this factory.
   *
   * @return the settings of this factory, never <code>null</code>.
   */
  public ISettings getSettings()
  {
    return settings;
  }

  /**
   * Creates a storage for the application identified by the given application token.
   * <p>
   * This factory searches for a {@link IStorage#getService() storage service} in the following order:
   * <p>
   * <ol>
   * <li> Look-up a mapping for the <code>applicationToken</code> in the {@link #getSettings() settings} of this factory.
   * <li> Look-up a mapping for <code>"&lt;default>"</code> in the {@link #getSettings() settings} of this factory.
   * <li> Look-up the first service in the {@link IStorageService.Registry storage service registry}.
   * <li> No service is assigned if the {@link Registry storage service registry} is empty.
   * </ol>
   * <p>
   * Calling this method is identical to calling <code>create(applicationToken, null)</code>.
   * <p>
   *
   * @param applicationToken the application token that identifies the application of the storage to be created.
   *        Minimal {@link BadApplicationTokenException#validate(String) lexical validation} is performed on the passed application token.<p>
   * @return the newly created storage, never <code>null</code>.<p>
   * @throws BadApplicationTokenException if {@link BadApplicationTokenException#validate(String) lexical validation} of the passed application token fails.<p>
   *
   * @see #create(String, StorageCache)
   */
  public IStorage create(String applicationToken) throws BadApplicationTokenException
  {
    return create(applicationToken, null);
  }

  /**
   * Creates a storage for the application identified by the given application token and associates it with a given {@link StorageCache storage cache}.
   * <p>
   * This factory searches for a {@link IStorage#getService() storage service} in the following order:
   * <p>
   * <ol>
   * <li> Look-up a mapping for the <code>applicationToken</code> in the {@link #getSettings() settings} of this factory.
   * <li> Look-up a mapping for <code>"&lt;default>"</code> in the {@link #getSettings() settings} of this factory.
   * <li> Look-up the first service in the {@link IStorageService.Registry storage service registry}.
   * <li> No service is assigned if the {@link Registry storage service registry} is empty.
   * </ol>
   * <p>
   *
   * @param applicationToken the application token that identifies the application of the storage to be created.
   *        Minimal {@link BadApplicationTokenException#validate(String) lexical validation} is performed on the passed application token.<p>
   * @param cache a local storage cache to be used as a locally persistent optimization, or <code>null</code> if local caching is not wanted.<p>
   * @return the newly created storage, never <code>null</code>.<p>
   * @throws BadApplicationTokenException if {@link BadApplicationTokenException#validate(String) lexical validation} of the passed application token fails.<p>
   *
   * @see #create(String)
   * @see StorageCache
   */
  public IStorage create(String applicationToken, StorageCache cache) throws BadApplicationTokenException
  {
    return new Storage(this, applicationToken, cache);
  }
}
