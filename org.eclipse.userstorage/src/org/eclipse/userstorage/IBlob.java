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
import org.eclipse.userstorage.util.ConflictException;
import org.eclipse.userstorage.util.ProtocolException;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Represents a piece of data that a {@link #getStorage() storage}
 * maintains for the logged-in user under the associated {@link #getKey() key}.
 * <p>
 * The actual data can be read from the {@link InputStream} returned by the {@link #getContents() getContents()} method.
 * It can be written by passing an {@link InputStream} to the {@link #setContents(InputStream) setContents()} method.
 * <p>
 * On top of this scalable I/O model a few methods are provided to get or set the blob's contents in
 * more convenient formats, such as {@link DataInput#readUTF() UTF strings}, integers, or booleans.
 * <p>
 * In no case the contents of this blob are kept in memory. If the {@link #getStorage() storage} of this blob was
 * created with a {@link StorageFactory#create(String, StorageCache) storage cache}
 * the contents are possibly served from that cache. In any case all read and write access contacts the remote service
 * behind storage's service, even if only to validate that a possibly cached version of the contents is still up-to-date,
 * see {@link #getETag()}.
 * <p>
 *
 * @author Eike Stepper
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBlob
{
  /**
   * Returns the storage of this blob.
   *
   * @return the storage of this blob, never <code>null</code>.
   */
  public IStorage getStorage();

  /**
   * Returns the key of this blob.
   *
   * @return the key of this blob, never <code>null</code>.
   */
  public String getKey();

  /**
   * Returns an immutable {@link Map} of the properties of this blob.
   * <p>
   * The properties represent arbitrary meta data about this blob that the service provider decides to provide.
   * The only property that service providers are required to provide is the {@link #getETag() ETag} property.
   * <p>
   * The properties map is only updated during any of the getContents() or setContents() methods.
   * If the {@link #getStorage() storage} of this blob was
   * created with a {@link StorageFactory#create(String, StorageCache) storage cache}
   * the properties are possibly served from that cache.
   * <p>
   *
   * @return an immutable {@link Map} of the properties of this blob, never <code>null</code>.<p>
   * @throws IllegalStateException if this blob is {@link #isDisposed() disposed}.<p>
   *
   * @see #getETag()
   * @see #getContents()
   * @see #setContents(InputStream)
   */
  public Map<String, String> getProperties() throws IllegalStateException;

  /**
   * Returns the ETag of this blob.
   * <p>
   * The ETag of a blob is an opaque but unique value that the service provider assigns to a particular version
   * (of the contents) of the blob. This value serves two purposes:
   * <p>
   * <ul>
   * <li> If the {@link #getStorage() storage} of this blob was created with a
   *      {@link StorageFactory#create(String, StorageCache) storage cache}
   *      and the cache contains the current version of this blob
   *      all getContents() methods use the ETag to prevent the server from sending the blob's contents again.
   *      The cached contents are returned instead.
   * <li> All setContents() methods use the ETag (if it is known) to prevent the server from storing conflicting
   *      changes. A conflicting change is updating a blob with new contents that is based on an out-dated ETag.
   * </ul>
   * <p>
   *
   * @return the ETag of this blob, or <code>null</code> if the ETag of this blob is currently not known.<p>
   * @throws IllegalStateException if this blob is {@link #isDisposed() disposed}.<p>
   *
   * @see #setETag(String)
   */
  public String getETag() throws IllegalStateException;

  /**
   * Sets the ETag of this blob.
   * <p>
   * The ETag of a blob is an opaque but unique value that the service provider assigns to a particular version
   * (of the contents) of the blob. This value serves two purposes:
   * <p>
   * <ul>
   * <li> If the {@link #getStorage() storage} of this blob was created with a
   *      {@link StorageFactory#create(String, StorageCache) storage cache}
   *      and the cache contains the current version of this blob
   *      all getContents() methods use the ETag to prevent the server from sending the blob's contents again.
   *      The cached contents are returned instead.
   * <li> All setContents() methods use the ETag (if it is known) to prevent the server from storing conflicting
   *      changes. A conflicting change is updating a blob with new contents that is based on an out-dated ETag.
   * </ul>
   * <p>
   * If the {@link #getStorage() storage} of this blob was created with a
   * {@link StorageFactory#create(String, StorageCache) storage cache}
   * and the new ETag is different from the cached ETag the cached blob is deleted locally.
   * <p>
   *
   * @param eTag the new ETag of this blob, or <code>null</code> to reset the ETag.<p>
   * @throws IllegalStateException if this blob is {@link #isDisposed() disposed}.<p>
   *
   * @see #getETag()
   */
  public void setETag(String eTag) throws IllegalStateException;

  /**
   * Returns an {@link InputStream} that represents the current contents of this blob.
   * <p>
   * This method always contacts the server.
   * <p>
   * If the {@link #getStorage() storage} of this blob was created with a
   * {@link StorageFactory#create(String, StorageCache) storage cache}
   * and the cache contains the current version of this blob (i.e., the blob's ETag is up-to-date)
   * the server will not send the blob's contents again. The cached contents are returned instead.
   * If the cache does not contain the current version of this blob (i.e., the blob's ETag is out-of-date)
   * the current remote contents are downloaded, cached, and returned by this method.
   * <p>
   * <b>Important note:</b>
   * <p>
   * The new contents will only be cached if (caching is enabled and) the
   * returned input stream is fully read and closed.
   * <p>
   *
   * @return an {@link InputStream} that represents the current contents of this blob, or <code>null</code> if this blob does not exist on the server.<p>
   * @throws IOException if remote I/O was unsuccessful. A {@link ProtocolException} may contain more information about protocol-specific problems.<p>
   * @throws IllegalStateException if this blob is {@link #isDisposed() disposed}.<p>
   *
   * @see #setContents(InputStream)
   * @see #getETag()
   */
  public InputStream getContents() throws IOException, IllegalStateException;

  /**
   * Sets an {@link InputStream} that represents the new contents of this blob.
   * <p>
   * This method always contacts the server.
   * <p>
   * If this blob has an ETag it will be used to detect and avoid conflicts on the server side.
   * The ETag of this blob can be {@link #setETag(String) set} to <code>null</code> if server-side conflict detection is not desired.
   * Server-side conflicts are indicated by throwing a {@link ConflictException}.
   * <p>
   * If the {@link #getStorage() storage} of this blob was created with a
   * {@link StorageFactory#create(String, StorageCache) storage cache}
   * and the server successfully updated the blob (i.e., the blob's ETag was up-to-date)
   * the cache will be updated with the new version of the contents.
   * <p>
   *
   * @param in an {@link InputStream} that represents the new contents of this blob.<p>
   * @return <code>true</code> if a new blob was created, <code>false</code> if an existing blob was updated.<p>
   * @throws IOException if remote I/O was unsuccessful. A {@link ProtocolException} may contain more information about protocol-specific problems.<p>
   * @throws ConflictException if the server detected a conflict and did not update the blob.<p>
   * @throws IllegalStateException if this blob is {@link #isDisposed() disposed}.<p>
   *
   * @see #getContents()
   * @see #setETag(String)
   */
  public boolean setContents(InputStream in) throws IOException, ConflictException, IllegalStateException;

  /**
   * Returns a {@link String} that represents the current contents of this blob.
   * <p>
   * This method is a convenient wrapper around the {@link #getContents()} method.
   * It assumes that the binary contents of this blob represent a UTF-8 encoded text value.
   * If this assumption is wrong (and this method can not detect this case) the results are unpredictable.
   * Otherwise the semantics of this method are identical to the ones of {@link #getContents()}.
   * <p>
   *
   * @return a {@link String} that represents the current contents of this blob, <code>null</code> if this blob does not exist on the server.<p>
   * @throws IOException if remote I/O was unsuccessful. A {@link ProtocolException} may contain more information about protocol-specific problems.<p>
   * @throws IllegalStateException if this blob is {@link #isDisposed() disposed}.<p>
   *
   * @see #getContents()
   */
  public String getContentsUTF() throws IOException, IllegalStateException;

  /**
   * Sets a {@link String} that represents the new contents of this blob.
   * <p>
   * This method is a convenient wrapper around the {@link #setContents(InputStream)} method.
   * The semantics of this method are identical to the ones of {@link #setContents(InputStream)}.
   * <p>
   *
   * @param value a {@link String} that represents the new contents of this blob.<p>
   * @return <code>true</code> if a new blob was created, <code>false</code> if an existing blob was updated.<p>
   * @throws IOException if remote I/O was unsuccessful. A {@link ProtocolException} may contain more information about protocol-specific problems.<p>
   * @throws ConflictException if the server detected a conflict and did not update the blob.<p>
   * @throws IllegalStateException if this blob is {@link #isDisposed() disposed}.<p>
   *
   * @see #setContents(InputStream)
   */
  public boolean setContentsUTF(String value) throws IOException, ConflictException, IllegalStateException;

  /**
   * Returns a primitive int value that represents the current contents of this blob.
   * <p>
   * This method is a convenient wrapper around the {@link #getContents()} method.
   * It assumes that the binary contents of this blob represent a UTF-8 encoded integer value.
   * If this assumption is wrong (and this method can not detect this case) the results are unpredictable.
   * Otherwise the semantics of this method are identical to the ones of {@link #getContents()}.
   * <p>
   *
   * @return a primitive int value that represents the current contents of this blob, <code>null</code> if this blob does not exist on the server.<p>
   * @throws IOException if remote I/O was unsuccessful. A {@link ProtocolException} may contain more information about protocol-specific problems.<p>
   * @throws IllegalStateException if this blob is {@link #isDisposed() disposed}.<p>
   *
   * @see #getContents()
   */
  public int getContentsInt() throws IOException, NumberFormatException, IllegalStateException;

  /**
   * Sets a a primitive int value that represents the new contents of this blob.
   * <p>
   * This method is a convenient wrapper around the {@link #setContents(InputStream)} method.
   * The semantics of this method are identical to the ones of {@link #setContents(InputStream)}.
   * <p>
   *
   * @param value a a primitive int value that represents the new contents of this blob.<p>
   * @return <code>true</code> if a new blob was created, <code>false</code> if an existing blob was updated.<p>
   * @throws IOException if remote I/O was unsuccessful. A {@link ProtocolException} may contain more information about protocol-specific problems.<p>
   * @throws ConflictException if the server detected a conflict and did not update the blob.<p>
   * @throws IllegalStateException if this blob is {@link #isDisposed() disposed}.<p>
   *
   * @see #setContents(InputStream)
   */
  public boolean setContentsInt(int value) throws IOException, ConflictException, IllegalStateException;

  /**
   * Returns a primitive boolean value that represents the current contents of this blob.
   * <p>
   * This method is a convenient wrapper around the {@link #getContents()} method.
   * It assumes that the binary contents of this blob represent a UTF-8 encoded boolean value.
   * If this assumption is wrong (and this method can not detect this case) the results are unpredictable.
   * Otherwise the semantics of this method are identical to the ones of {@link #getContents()}.
   * <p>
   *
   * @return a primitive boolean value that represents the current contents of this blob, <code>null</code> if this blob does not exist on the server.<p>
   * @throws IOException if remote I/O was unsuccessful. A {@link ProtocolException} may contain more information about protocol-specific problems.<p>
   * @throws IllegalStateException if this blob is {@link #isDisposed() disposed}.<p>
   *
   * @see #getContents()
   */
  public boolean getContentsBoolean() throws IOException, IllegalStateException;

  /**
   * Sets a a primitive boolean value that represents the new contents of this blob.
   * <p>
   * This method is a convenient wrapper around the {@link #setContents(InputStream)} method.
   * The semantics of this method are identical to the ones of {@link #setContents(InputStream)}.
   * <p>
   *
   * @param value a a primitive boolean value that represents the new contents of this blob.<p>
   * @return <code>true</code> if a new blob was created, <code>false</code> if an existing blob was updated.<p>
   * @throws IOException if remote I/O was unsuccessful. A {@link ProtocolException} may contain more information about protocol-specific problems.<p>
   * @throws ConflictException if the server detected a conflict and did not update the blob.<p>
   * @throws IllegalStateException if this blob is {@link #isDisposed() disposed}.<p>
   *
   * @see #setContents(InputStream)
   */
  public boolean setContentsBoolean(boolean value) throws IOException, ConflictException, IllegalStateException;

  /**
   * Returns <code>true</code> if this blob is disposed, <code>false</code> otherwise.
   * <p>
   * The only way for a blob to become disposed is when the storage of the {@link IStorage storage} of that blob
   * is changed after that blob was created. Applications must call {@link IStorage#getBlob(String) getBlob()} to acquire
   * a new, valid blob instance in that case.
   * <p>
   *
   * @see IStorage#getBlob(String) getBlob()
   */
  public boolean isDisposed();
}
