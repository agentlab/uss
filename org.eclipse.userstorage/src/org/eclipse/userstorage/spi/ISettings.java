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
import org.eclipse.userstorage.IStorageService;

/**
 * A generic key/value map used by the {@link StorageFactory#DEFAULT default storage factory}
 * to remember the last {@link IStorageService service} of a {@link IStorage storage}.
 *
 * @author Eike Stepper
 */
public interface ISettings
{
  /**
   * Returns the value for the given key.
   *
   * @param key the key for which to return the value.<p>
   * @return the value for the given key.<p>
   * @throws Exception in case of any problems with the settings backing store.<p>
   */
  public String getValue(String key) throws Exception;

  /**
   * Sets the value for the given key.
   *
   * @param key the key for which to set the value.<p>
   * @param value the value  to set or <code>null</code> to remove the mapping.<p>
   * @throws Exception in case of any problems with the settings backing store.<p>
   */
  public void setValue(String key, String value) throws Exception;
}
