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
package org.eclipse.userstorage.internal.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * @author Eike Stepper
 */
public final class IOUtil
{
  private static final byte[] BUFFER = new byte[8192];

  private IOUtil()
  {
  }

  public static void mkdirs(File folder) throws RuntimeException
  {
    if (folder != null)
    {
      if (!folder.isDirectory())
      {
        if (!folder.mkdirs())
        {
          throw new RuntimeException("Unable to create directory " + folder.getAbsolutePath()); //$NON-NLS-1$
        }
      }
    }
  }

  public static void delete(File file)
  {
    if (file != null)
    {
      if (file.isDirectory())
      {
        deleteFiles(file);
      }

      file.delete();
    }
  }

  public static void deleteFiles(File folder)
  {
    if (folder != null)
    {
      File[] files = folder.listFiles();
      if (files != null)
      {
        for (File file : files)
        {
          delete(file);
        }
      }
    }
  }

  public static long copy(InputStream input, OutputStream output, byte buffer[]) throws RuntimeException
  {
    try
    {
      long length = 0;
      int n;

      while ((n = input.read(buffer)) != -1)
      {
        output.write(buffer, 0, n);
        length += n;
      }

      return length;
    }
    catch (IOException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  public static long copy(InputStream input, OutputStream output, int bufferSize) throws RuntimeException
  {
    if (bufferSize == BUFFER.length)
    {
      return copy(input, output);
    }

    return copy(input, output, new byte[bufferSize]);
  }

  public static long copy(InputStream input, OutputStream output) throws RuntimeException
  {
    synchronized (BUFFER)
    {
      return copy(input, output, BUFFER);
    }
  }

  public static Object readObject(File file)
  {
    InputStream inputStream = null;

    try
    {
      inputStream = new FileInputStream(file);
      ObjectInputStream ois = new ObjectInputStream(inputStream);
      return ois.readObject();
    }
    catch (RuntimeException ex)
    {
      throw ex;
    }
    catch (Error ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex);
    }
    finally
    {
      close(inputStream);
    }
  }

  public static void writeObject(File file, Object object)
  {
    mkdirs(file.getParentFile());

    OutputStream outputStream = null;

    try
    {
      outputStream = new FileOutputStream(file);

      @SuppressWarnings("resource")
      ObjectOutputStream oos = new ObjectOutputStream(outputStream);
      oos.writeObject(object);
      oos.flush();
    }
    catch (RuntimeException ex)
    {
      throw ex;
    }
    catch (Error ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex);
    }
    finally
    {
      close(outputStream);
    }
  }

  public static String readUTF(File file)
  {
    InputStream inputStream = null;

    try
    {
      inputStream = new FileInputStream(file);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      copy(inputStream, outputStream);

      return StringUtil.fromUTF(outputStream.toByteArray());
    }
    catch (RuntimeException ex)
    {
      throw ex;
    }
    catch (Error ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex);
    }
    finally
    {
      close(inputStream);
    }
  }

  public static void writeUTF(File file, String contents)
  {
    OutputStream outputStream = null;

    try
    {
      mkdirs(file.getParentFile());

      InputStream inputStream = new ByteArrayInputStream(StringUtil.toUTF(contents));
      outputStream = new FileOutputStream(file);

      copy(inputStream, outputStream);
    }
    catch (RuntimeException ex)
    {
      throw ex;
    }
    catch (Error ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex);
    }
    finally
    {
      close(outputStream);
    }
  }

  public static void close(Closeable closeable) throws RuntimeException
  {
    try
    {
      if (closeable != null)
      {
        closeable.close();
      }
    }
    catch (IOException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  public static IOException closeSilent(Closeable closeable)
  {
    try
    {
      if (closeable != null)
      {
        closeable.close();
      }
    }
    catch (IOException ex)
    {
      return ex;
    }

    return null;
  }

  /**
   * @author Eike Stepper
   */
  public interface EndOfFileAware
  {
    public void reachedEndOfFile();
  }

  /**
   * @author Eike Stepper
   */
  public static final class TeeInputStream extends FilterInputStream implements EndOfFileAware
  {
    private static final int EOF = -1;

    private final OutputStream out;

    public TeeInputStream(InputStream in, OutputStream out)
    {
      super(in);
      this.out = out;
    }

    @Override
    public int read() throws IOException
    {
      int c = in.read();
      if (c != EOF)
      {
        out.write(c);
      }
      else
      {
        reachedEndOfFile();
      }

      return c;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
      int n = in.read(b, off, len);
      if (n != EOF)
      {
        out.write(b, off, n);
      }
      else
      {
        reachedEndOfFile();
      }

      return n;
    }

    @Override
    public void reachedEndOfFile()
    {
      if (out instanceof IOUtil.EndOfFileAware)
      {
        IOUtil.EndOfFileAware endOfFileAware = (IOUtil.EndOfFileAware)out;
        endOfFileAware.reachedEndOfFile();
      }
    }

    @Override
    public void close() throws IOException
    {
      try
      {
        super.close();
      }
      finally
      {
        out.close();
      }
    }
  }
}
