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
import org.eclipse.userstorage.IStorageService.Registry;
import org.eclipse.userstorage.internal.DefaultStorageFactory;
import org.eclipse.userstorage.internal.InternalStorageFactory;
import org.eclipse.userstorage.util.BadApplicationTokenException;

import java.util.NoSuchElementException;

/**
 * Creates {@link IStorage storages}.
 *
 * @author Eike Stepper
 */
public class StorageFactory extends InternalStorageFactory
{
  public static final StorageFactory DEFAULT = new DefaultStorageFactory();

  /**
   * Constructs this storage factory.
   */
  public StorageFactory()
  {
  }

  /**
   * Creates a storage for the application identified by the given application token.
   * <p>
   * Calling this method is identical to calling <code>create(applicationToken, null)</code>.
   * <p>
   *
   * @param applicationToken the application token that identifies the application of the storage to be created.
   *        Minimal {@link BadApplicationTokenException#validate(String) lexical validation} is performed on the passed application token.<p>
   * @return the newly created storage, never <code>null</code>.<p>
   * @throws NoSuchElementException if the {@link Registry storage registry} is empty and, hence, there is no default storage available.<p>
   * @throws BadApplicationTokenException if {@link BadApplicationTokenException#validate(String) lexical validation} of the passed application token fails.<p>
   *
   * @see #create(String, StorageCache)
   */
  public final IStorage create(String applicationToken) throws NoSuchElementException, BadApplicationTokenException
  {
    return super.create(applicationToken, null);
  }

  /**
   * Creates a storage for the application identified by the given application token and associates it with a given {@link StorageCache storage cache}.
   * <p>
   * @param applicationToken the application token that identifies the application of the storage to be created.
   *        Minimal {@link BadApplicationTokenException#validate(String) lexical validation} is performed on the passed application token.<p>
   * @param cache a local storage cache to be used as a locally persistent optimization, or <code>null</code> if local caching is not wanted.<p>
   * @return the newly created storage, never <code>null</code>.<p>
   * @throws NoSuchElementException if the {@link Registry storage registry} is empty and, hence, there is no default storage available.<p>
   * @throws BadApplicationTokenException if {@link BadApplicationTokenException#validate(String) lexical validation} of the passed application token fails.<p>
   *
   * @see #create(String)
   * @see StorageCache
   */
  @Override
  public final IStorage create(String applicationToken, StorageCache cache) throws NoSuchElementException, BadApplicationTokenException
  {
    return super.create(applicationToken, cache);
  }

  /**
   * Returns the URI of the preferred service for the given application.
   * <p>
   * Subclasses can override this method, for example,  to implement a per-application service memory.
   * The default implementation returns <code>null</code>.
   * <p>
   *
   * @param applicationToken the application token.<p>
   * @return the URI of the preferred service for the given application.
   */
  @Override
  protected String getPreferredServiceURI(String applicationToken)
  {
    return null;
  }

  /**
   * Sets the URI of the preferred service for the given application.
   * <p>
   * Subclasses can override this method, for example,  to implement a per-application service memory.
   * The default implementation does nothing.
   * <p>
   *
   * @param applicationToken the application token.<p>
   * @param serviceURI the URI of the preferred service for the given application.
   */
  @Override
  protected void setPreferredServiceURI(String applicationToken, String serviceURI)
  {
    // Do nothing.
  }
}
