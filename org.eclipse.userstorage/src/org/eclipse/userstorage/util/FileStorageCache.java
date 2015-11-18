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
package org.eclipse.userstorage.util;

import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.spi.StorageCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A local file system based {@link StorageCache cache}.
 *
 * @author Eike Stepper
 */
public class FileStorageCache extends StorageCache
{
  /**
   * The file name extension of the files that contain the blobs' properties.
   */
  protected static final String PROPERTIES = ".properties";

  private final File folder;

  /**
   * Constructs this cache with a randomly named folder in the user's temp directory.
   *
   * @see #getFolder()
   */
  public FileStorageCache()
  {
    this(createTempFolder());
  }

  /**
   * Constructs this cache with the given folder.
   *
   * @param folder the folder in which to create this cache, must not be <code>null</code>.<p>
   *
   * @see #getFolder()
   */
  public FileStorageCache(File folder)
  {
    if (folder == null)
    {
      throw new IllegalArgumentException("Folder is null");
    }

    this.folder = folder;
  }

  /**
   * Returns the file system folder of this cache.
   *
   * @return the file system folder of this cache, never <code>null</code>.<p>
   */
  public final File getFolder()
  {
    return folder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<String> getKeys(String applicationToken) throws IOException
  {
    Set<String> keys = new HashSet<String>();

    File applicationFolder = getApplicationFolder(applicationToken);
    File[] files = applicationFolder.listFiles();
    if (files != null)
    {
      for (File file : files)
      {
        String name = file.getName();

        String key = getKeyFromFileName(name);
        if (key != null)
        {
          keys.add(name);
        }
      }
    }

    return keys.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void loadProperties(String applicationToken, String key, Map<String, String> properties) throws IOException
  {
    File file = getFile(applicationToken, key, PROPERTIES);
    if (file.isFile())
    {
      InputStream in = null;

      try
      {
        in = new FileInputStream(file);

        Properties buffer = new Properties();
        buffer.load(in);

        for (Map.Entry<Object, Object> entry : buffer.entrySet())
        {
          properties.put((String)entry.getKey(), (String)entry.getValue());
        }
      }
      finally
      {
        IOUtil.close(in);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveProperties(String applicationToken, String key, Map<String, String> properties) throws IOException
  {
    File file = getFile(applicationToken, key, PROPERTIES);

    Properties buffer = new Properties();
    for (Map.Entry<String, String> entry : properties.entrySet())
    {
      buffer.put(entry.getKey(), entry.getValue());
    }

    OutputStream out = null;

    try
    {
      IOUtil.mkdirs(file.getParentFile());

      out = new FileOutputStream(file);
      buffer.store(out, "Blob " + applicationToken + "/" + key);
    }
    finally
    {
      IOUtil.close(out);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected InputStream getInputStream(String applicationToken, String key) throws IOException
  {
    File file = getFile(applicationToken, key, null);
    return new FileInputStream(file);
  }

  /**
  * {@inheritDoc}
  */
  @Override
  protected OutputStream getOutputStream(String applicationToken, String key) throws IOException
  {
    File file = getFile(applicationToken, key, null);
    IOUtil.mkdirs(file.getParentFile());
    return new FileOutputStream(file);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void delete(String applicationToken, String key) throws IOException
  {
    getFile(applicationToken, key, PROPERTIES).delete();
    getFile(applicationToken, key, null).delete();
  }

  /**
   * Returns the {@link File} object that represents the blob with the given
   * {@link IStorage#getApplicationToken() application token} and {@link IBlob#getKey() key} into this cache.
   * <p>
   * This cache stores the properties and the contents of a blob in two different files.
   * Which of the two files is returned is determined by the value of the extension parameter.
   * <p>
   *
   * @param applicationToken the {@link IStorage#getApplicationToken() application token} for which to return the file,
   *        must not be <code>null</code>.<p>
   * @param key the {@link IBlob#getKey() key} for which to return the file,
   *        must not be <code>null</code>.<p>
   * @param extension the value of {@link #PROPERTIES} to return the file that contains the blob's properties,
   *        or <code>null</code> to return the file that contains the blob's contents.<p>
   * @return the file that contains the blob's properties if the value of {@link #PROPERTIES} was passed into the <code>extension</code> parameter,
   *         or the file that contains the blob's contents if <code>null</code> was passed into the <code>extension</code> parameter.<p>
   */
  protected File getFile(String applicationToken, String key, String extension)
  {
    File applicationFolder = getApplicationFolder(applicationToken);
    String keyFileName = getFileNameFromKey(key, extension);
    return new File(applicationFolder, keyFileName);
  }

  protected File getApplicationFolder(String applicationFolderName)
  {
    return new File(folder, applicationFolderName);
  }

  protected String getFileNameFromKey(String key, String extension)
  {
    return key + StringUtil.safe(extension);
  }

  protected String getKeyFromFileName(String name)
  {
    if (name.endsWith(PROPERTIES))
    {
      return name.substring(0, name.length() - PROPERTIES.length());
    }

    return null;
  }

  private static File createTempFolder()
  {
    for (int i = 0; i < 100; i++)
    {
      try
      {
        File folder = File.createTempFile("userstorage-", "");
        if (!folder.delete())
        {
          continue;
        }

        IOUtil.mkdirs(folder);
        return folder;
      }
      catch (Exception ex)
      {
        //$FALL-THROUGH$
      }
    }

    throw new RuntimeException("Temporary folder could not be created");
  }

  /**
   * @author Eike Stepper
   */
  public static class SingleApplication extends FileStorageCache
  {
    private final String applicationToken;

    public SingleApplication(String applicationToken)
    {
      this.applicationToken = applicationToken;
    }

    public SingleApplication(File folder, String applicationToken)
    {
      super(folder);
      this.applicationToken = applicationToken;
    }

    public final String getApplicationToken()
    {
      return applicationToken;
    }

    @Override
    public Iterator<String> getKeys(String applicationToken) throws IOException
    {
      checkApplication(applicationToken);
      return super.getKeys(applicationToken);
    }

    @Override
    protected void loadProperties(String applicationToken, String key, Map<String, String> properties) throws IOException
    {
      checkApplication(applicationToken);
      super.loadProperties(applicationToken, key, properties);
    }

    @Override
    protected void saveProperties(String applicationToken, String key, Map<String, String> properties) throws IOException
    {
      checkApplication(applicationToken);
      super.saveProperties(applicationToken, key, properties);
    }

    @Override
    protected InputStream getInputStream(String applicationToken, String key) throws IOException
    {
      checkApplication(applicationToken);
      return super.getInputStream(applicationToken, key);
    }

    @Override
    protected OutputStream getOutputStream(String applicationToken, String key) throws IOException
    {
      checkApplication(applicationToken);
      return super.getOutputStream(applicationToken, key);
    }

    @Override
    protected void delete(String applicationToken, String key) throws IOException
    {
      checkApplication(applicationToken);
      super.delete(applicationToken, key);
    }

    @Override
    protected File getFile(String applicationToken, String key, String extension)
    {
      checkApplication(applicationToken);
      return super.getFile(applicationToken, key, extension);
    }

    @Override
    protected File getApplicationFolder(String applicationFolderName)
    {
      checkApplication(applicationToken);
      return getFolder();
    }

    protected final void checkApplication(String applicationToken) throws IllegalArgumentException
    {
      if (!this.applicationToken.equals(applicationToken))
      {
        throw new IllegalArgumentException("Invalid application token: " + applicationToken);
      }
    }

    /**
     * @author Eike Stepper
     */
    public static class SingleKey extends FileStorageCache
    {
      private static final Set<String> NO_KEYS = Collections.emptySet();

      private final String key;

      private String fileNamePrefix;

      public SingleKey(String key)
      {
        this.key = key;
        fileNamePrefix = key;
      }

      public SingleKey(File folder, String key)
      {
        super(folder);
        this.key = key;
        fileNamePrefix = key;
      }

      public final String getKey()
      {
        return key;
      }

      public String getFileNamePrefix()
      {
        return fileNamePrefix;
      }

      public void setFileNamePrefix(String fileNamePrefix)
      {
        this.fileNamePrefix = fileNamePrefix;
      }

      @Override
      public Iterator<String> getKeys(String applicationToken) throws IOException
      {
        for (Iterator<String> it = super.getKeys(applicationToken); it.hasNext();)
        {
          String key = it.next();
          if (this.key.equals(key))
          {
            return Collections.singleton(key).iterator();
          }
        }

        return NO_KEYS.iterator();
      }

      @Override
      protected void loadProperties(String applicationToken, String key, Map<String, String> properties) throws IOException
      {
        checkKey(key);
        super.loadProperties(applicationToken, key, properties);
      }

      @Override
      protected void saveProperties(String applicationToken, String key, Map<String, String> properties) throws IOException
      {
        checkKey(key);
        super.saveProperties(applicationToken, key, properties);
      }

      @Override
      protected InputStream getInputStream(String applicationToken, String key) throws IOException
      {
        checkKey(key);
        return super.getInputStream(applicationToken, key);
      }

      @Override
      protected OutputStream getOutputStream(String applicationToken, String key) throws IOException
      {
        checkKey(key);
        return super.getOutputStream(applicationToken, key);
      }

      @Override
      protected void delete(String applicationToken, String key) throws IOException
      {
        checkKey(key);
        super.delete(applicationToken, key);
      }

      @Override
      protected File getFile(String applicationToken, String key, String extension)
      {
        checkKey(key);
        return super.getFile(applicationToken, key, extension);
      }

      @Override
      protected String getFileNameFromKey(String key, String extension)
      {
        checkKey(key);
        return super.getFileNameFromKey(fileNamePrefix, extension);
      }

      @Override
      protected String getKeyFromFileName(String name)
      {
        String fileNamePrefix = super.getKeyFromFileName(name);
        if (this.fileNamePrefix.equals(fileNamePrefix))
        {
          return key;
        }

        return null;
      }

      protected final void checkKey(String key) throws IllegalArgumentException
      {
        if (!this.key.equals(key))
        {
          throw new IllegalArgumentException("Invalid key: " + key);
        }
      }
    }
  }
}
