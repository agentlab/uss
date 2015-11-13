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

import org.apache.commons.codec.binary.Base64InputStream;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Vector;

/**
 * A scalable JSON Codec.
 *
 * See http://www.json.org
 *
 * @author Eike Stepper
 */
public final class JSONUtil
{
  private static final boolean DEBUG = Boolean.getBoolean(JSONUtil.class.getName() + ".debug");

  private JSONUtil()
  {
  }

  private static String escape(String str)
  {
    if (str == null)
    {
      return null;
    }

    int len = str.length();
    StringBuilder builder = new StringBuilder(len);

    for (int i = 0; i < len; i++)
    {
      char c = str.charAt(i);

      if (c > 0xfff)
      {
        builder.append("\\u" + StringUtil.charToHex(c));
      }
      else if (c > 0xff)
      {
        builder.append("\\u0" + StringUtil.charToHex(c));
      }
      else if (c > 0x7f)
      {
        builder.append("\\u00" + StringUtil.charToHex(c));
      }
      else if (c < 32)
      {
        switch (c)
        {
          case '\r':
            builder.append('\\');
            builder.append('r');
            break;

          case '\n':
            builder.append('\\');
            builder.append('n');
            break;

          case '\t':
            builder.append('\\');
            builder.append('t');
            break;

          case '\f':
            builder.append('\\');
            builder.append('f');
            break;

          case '\b':
            builder.append('\\');
            builder.append('b');
            break;

          default:
            if (c > 0xf)
            {
              builder.append("\\u00" + StringUtil.charToHex(c));
            }
            else
            {
              builder.append("\\u000" + StringUtil.charToHex(c));
            }
        }
      }
      else if (c == '\\')
      {
        builder.append('\\');
        builder.append('\\');
      }
      else if (c == '"')
      {
        builder.append('\\');
        builder.append('"');
      }
      else
      {
        builder.append(c);
      }
    }

    return builder.toString();
  }

  public static InputStream encode(Map<String, String> properties, String streamKey, InputStream in)
  {
    StringBuilder builder = new StringBuilder();
    builder.append("{");

    boolean first = true;
    for (Map.Entry<String, String> entry : properties.entrySet())
    {
      if (first)
      {
        first = false;
      }
      else
      {
        builder.append(",");
      }

      builder.append("\"");
      builder.append(entry.getKey());
      builder.append("\":\"");
      builder.append(escape(entry.getValue()));
      builder.append("\"");
    }

    if (in == null)
    {
      builder.append("}");
      InputStream result = new ByteArrayInputStream(StringUtil.toUTF(builder.toString()));

      if (DEBUG)
      {
        return new DebugInputStream(result, "ENCODE ");
      }

      return result;
    }

    if (!first)
    {
      builder.append(",");
    }

    builder.append("\"");
    builder.append(streamKey);
    builder.append("\":\"");

    Vector<InputStream> streams = new Vector<InputStream>(3);
    streams.add(new ByteArrayInputStream(StringUtil.toUTF(builder.toString())));
    streams.add(new Base64InputStream(in, true, Integer.MAX_VALUE, null));
    streams.add(new ByteArrayInputStream(StringUtil.toUTF("\"}")));

    InputStream result = new SequenceInputStream(streams.elements());

    if (DEBUG)
    {
      result = new DebugInputStream(result, "ENCODE-STREAM ");
    }

    return result;
  }

  public static InputStream decode(InputStream in, Map<String, String> properties, String streamKey) throws IOException
  {
    if (DEBUG)
    {
      System.out.print(streamKey != null ? "DECODE-STREAM " : "DECODE ");
    }

    JSONParser parser = new JSONParser(in);
    return parser.parse(properties, streamKey);
  }

  /**
   * @author Eike Stepper
   */
  private static final class DebugInputStream extends FilterInputStream
  {
    private final String prefix;

    private boolean prefixDumped;

    public DebugInputStream(InputStream in, String prefix)
    {
      super(in);
      this.prefix = prefix;
    }

    @Override
    public int read() throws IOException
    {
      int c = super.read();
      if (c != -1)
      {
        dump(new byte[] { (byte)c }, 0, 1);
      }

      return c;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
      int n = super.read(b, off, len);
      dump(b, off, n);
      return n;
    }

    @Override
    public void close() throws IOException
    {
      System.out.println();
      super.close();
    }

    private void dump(byte[] b, int off, int len)
    {
      if (!prefixDumped)
      {
        if (prefix != null)
        {
          System.out.print(prefix);
        }

        prefixDumped = true;
      }

      if (len > 0)
      {
        for (int i = off; i < len; i++)
        {
          byte c = b[i];
          System.out.print((char)c);
        }
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  private static final class JSONParser
  {
    private static final char[] CONTROL_CHARS = { '"', '\\', '/', 'b', 'f', 'n', 'r', 't', 'u' };

    private static final char[] OBJECT_CHARS = { '"', '{', '[', 'n', 't', 'f', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '}' };

    private static final char[] ARRAY_CHARS = { '"', '{', '[', 'n', 't', 'f', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ']' };

    private static final char[] NUMBER_CHARS = { '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', 'e', 'E' };

    private Reader reader;

    private int lookAhead = -1;

    private int pos;

    public JSONParser(InputStream in)
    {
      try
      {
        reader = new InputStreamReader(in, StringUtil.UTF8);
      }
      catch (UnsupportedEncodingException ex)
      {
        throw new RuntimeException(ex);
      }
    }

    public InputStream parse(Map<String, String> properties, String streamKey) throws IOException
    {
      char c = skipWhitespace('{', '[');
      if (c == '{')
      {
        InputStream result = parseObject(properties, streamKey);

        if (DEBUG)
        {
          if (streamKey == null)
          {
            System.out.println();
          }
        }

        return result;
      }

      // parseArray();
      return null;
    }

    private InputStream parseObject(Map<String, String> properties, String streamKey) throws IOException
    {
      for (;;)
      {
        char c = skipWhitespace('"');

        String key = readText(false);

        c = skipWhitespace(':');
        c = skipWhitespace(OBJECT_CHARS);

        if (c == '}')
        {
          return null;
        }
        else if (c == '{')
        {
          parseObject(null, null);
        }
        else if (c == '[')
        {
          parseArray();
        }
        else if (c == '"')
        {
          if (key.equals(streamKey))
          {
            // Store result in local variable to avoid resource leak warning at the "return null" statement below.
            InputStream result = new Base64InputStream(new ValueInputStream());

            return result;
          }

          String value = readText(true);
          put(properties, key, value);
        }
        else
        {
          parseLiteral(properties, key, c);
        }

        c = skipWhitespace(',', '}');
        if (c == '}')
        {
          return null;
        }
      }
    }

    private void parseArray() throws IOException
    {
      for (;;)
      {
        char c = skipWhitespace(ARRAY_CHARS);

        if (c == ']')
        {
          return;
        }

        if (c == '{')
        {
          parseObject(null, null);
        }
        else if (c == '[')
        {
          parseArray();
        }
        else if (c == '"')
        {
          readText(false);
        }
        else
        {
          parseLiteral(null, null, c);
        }
        if (c == 'n')
        {
          expectChar(readChar(), 'u');
          expectChar(readChar(), 'l');
          expectChar(readChar(), 'l');
        }
        else if (c == 't')
        {
          expectChar(readChar(), 'r');
          expectChar(readChar(), 'u');
          expectChar(readChar(), 'e');
        }
        else if (c == 'f')
        {
          expectChar(readChar(), 'a');
          expectChar(readChar(), 'l');
          expectChar(readChar(), 's');
          expectChar(readChar(), 'e');
        }

        c = skipWhitespace(',', ']');
        if (c == ']')
        {
          return;
        }
      }
    }

    private void parseLiteral(Map<String, String> properties, String key, char c) throws IOException
    {
      if (c == 'n')
      {
        expectChar(readChar(), 'u');
        expectChar(readChar(), 'l');
        expectChar(readChar(), 'l');
        put(properties, key, "null");
      }
      else if (c == 't')
      {
        expectChar(readChar(), 'r');
        expectChar(readChar(), 'u');
        expectChar(readChar(), 'e');
        put(properties, key, "true");
      }
      else if (c == 'f')
      {
        expectChar(readChar(), 'a');
        expectChar(readChar(), 'l');
        expectChar(readChar(), 's');
        expectChar(readChar(), 'e');
        put(properties, key, "false");
      }
      else if (c == '-' || c >= '0' && c <= '9')
      {
        StringBuilder builder = new StringBuilder();
        builder.append(c);

        while (isNumberChar(c = readChar()))
        {
          builder.append(c);
        }

        rememberChar(c);
        put(properties, key, builder.toString());
      }
    }

    private void put(Map<String, String> properties, String key, String value)
    {
      if (properties != null)
      {
        properties.put(key, value);
      }
    }

    private char skipWhitespace(char... expectedChars) throws IOException
    {
      char c = skipWhitespace();
      expectChar(c, expectedChars);
      return c;
    }

    private char skipWhitespace() throws IOException
    {
      char c = readChar();
      while (Character.isWhitespace(c))
      {
        c = readChar();
      }

      return c;
    }

    private String readText(boolean unescape) throws IOException
    {
      StringBuilder builder = new StringBuilder();

      char c = readChar();
      while (c != '"')
      {
        if (unescape && c == '\\')
        {
          c = readChar();
          switch (c)
          {
            case '"':
            case '\\':
            case '/':
              builder.append(c);
              break;

            case 'b':
              builder.append('\b');
              break;

            case 'f':
              builder.append('\f');
              break;

            case 'n':
              builder.append('\n');
              break;

            case 'r':
              builder.append('\r');
              break;

            case 't':
              builder.append('\t');
              break;

            case 'u':
              StringBuilder unicodeBuilder = new StringBuilder();
              unicodeBuilder.append(readChar());
              unicodeBuilder.append(readChar());
              unicodeBuilder.append(readChar());
              unicodeBuilder.append(readChar());
              String unicode = unicodeBuilder.toString();

              try
              {
                builder.append(StringUtil.hexToChar(unicode));
              }
              catch (NumberFormatException ex)
              {
                builder.append('\\');
                builder.append('u');
                builder.append(unicode);
              }
              break;

            default:
              expectChar(c, CONTROL_CHARS);
          }
        }

        builder.append(c);
        c = readChar();
      }

      return builder.toString();
    }

    private char readChar() throws IOException
    {
      pos++;

      int i = lookAhead;
      if (i == -1)
      {
        i = reader.read();
      }
      else
      {
        lookAhead = -1;
      }

      if (i == -1)
      {
        throw new EOFException();
      }

      char c = (char)i;

      if (DEBUG)
      {
        System.out.print(c);
      }

      return c;
    }

    private void rememberChar(char c)
    {
      lookAhead = c;
      --pos;
    }

    private void expectChar(char c, char... expectedChars) throws IOException
    {
      for (int i = 0; i < expectedChars.length; i++)
      {
        char expectedChar = expectedChars[i];
        if (c == expectedChar)
        {
          return;
        }
      }

      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < expectedChars.length; i++)
      {
        if (builder.length() != 0)
        {
          builder.append(" or ");
        }

        builder.append("'");
        builder.append(expectedChars[i]);
        builder.append("'");
      }

      throw new JSONParseException("Expected " + builder + " but found '" + c + "' at position " + pos);
    }

    private static boolean isNumberChar(char c)
    {
      for (int i = 0; i < NUMBER_CHARS.length; i++)
      {
        char numberChar = NUMBER_CHARS[i];
        if (c == numberChar)
        {
          return true;
        }
      }

      return false;
    }

    /**
     * @author Eike Stepper
     */
    private final class ValueInputStream extends InputStream
    {
      private boolean eof;

      @Override
      public int read() throws IOException
      {
        if (eof)
        {
          return -1;
        }

        int c = reader.read();

        if (DEBUG)
        {
          if (c != -1)
          {
            System.out.print((char)c);
          }
        }

        if (c == '"')
        {
          eof = true;
          return -1;
        }

        return c;
      }

      @Override
      public void close() throws IOException
      {
        if (DEBUG)
        {
          System.out.println("}");
        }

        reader.close();
        super.close();
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  public static final class JSONParseException extends IOException
  {
    private static final long serialVersionUID = 1L;

    public JSONParseException(String message)
    {
      super(message);
    }
  }
}
