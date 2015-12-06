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

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
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

  public static void dump(Object value)
  {
    JSONDumper dumper = new JSONDumper(System.out);
    dumper.dumpValue(value);
    System.out.println();
  }

  public static InputStream build(Object value)
  {
    JSONBuilder builder = new JSONBuilder();
    InputStream result = builder.buildValue(value);

    if (DEBUG)
    {
      result = new DebugInputStream(result, "ENCODE ");
    }

    return result;
  }

  public static <T> T parse(InputStream in, String streamKey) throws IOException
  {
    if (DEBUG)
    {
      System.out.print("DECODE ");
    }

    JSONParser parser = new JSONParser(in, streamKey);

    @SuppressWarnings("unchecked")
    T value = (T)parser.parseValue();
    return value;
  }

  /**
   * @author Eike Stepper
   */
  private static final class JSONDumper
  {
    private final PrintStream out;

    private int level;

    public JSONDumper(PrintStream out)
    {
      this.out = out;
    }

    public void dumpValue(Object value)
    {
      if (value instanceof Map)
      {
        @SuppressWarnings("unchecked")
        Map<String, Object> object = (Map<String, Object>)value;
        dumpObject(object);
      }
      else if (value instanceof List)
      {
        @SuppressWarnings("unchecked")
        List<Object> array = (List<Object>)value;
        dumpArray(array);
      }
      else if (value instanceof InputStream)
      {
        InputStream stream = (InputStream)value;
        dumpStream(stream);
      }
      else if (value instanceof String)
      {
        out.print("\"" + value + "\"");
      }
      else
      {
        out.print(String.valueOf(value));
      }
    }

    public void dumpObject(Map<String, Object> object)
    {
      indent();
      out.println("{");
      ++level;

      int i = 0;
      for (Map.Entry<String, Object> entry : object.entrySet())
      {
        indent();
        out.print(entry.getKey());
        out.print(" : ");
        dumpValue(entry.getValue());

        if (++i == object.size())
        {
          out.println();
        }
        else
        {
          out.println(",");
        }
      }

      --level;
      indent();
      out.print("}");
    }

    public void dumpArray(List<Object> array)
    {
      out.println("[");
      ++level;

      int i = 0;
      for (Object value : array)
      {
        dumpValue(value);

        if (++i == array.size())
        {
          out.println();
        }
        else
        {
          out.println(",");
        }
      }

      --level;
      indent();
      out.print("]");
    }

    public void dumpStream(InputStream stream)
    {
      out.print("STREAM");
    }

    private void indent()
    {
      for (int i = 0; i < level; i++)
      {
        out.print("  ");
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  private static final class JSONBuilder
  {
    public JSONBuilder()
    {
    }

    public InputStream buildValue(Object value)
    {
      if (value == null)
      {
        return IOUtil.streamUTF("null");
      }

      if (value == Boolean.TRUE)
      {
        return IOUtil.streamUTF("true");
      }

      if (value == Boolean.FALSE)
      {
        return IOUtil.streamUTF("false");
      }

      if (value.getClass() == Integer.class)
      {
        return IOUtil.streamUTF(Integer.toString((Integer)value));
      }

      if (value.getClass() == String.class)
      {
        return IOUtil.streamUTF("\"" + escape((String)value) + "\"");
      }

      if (value instanceof Map)
      {
        @SuppressWarnings("unchecked")
        Map<String, Object> object = (Map<String, Object>)value;
        return buildObject(object);
      }

      if (value instanceof List)
      {
        @SuppressWarnings("unchecked")
        List<Object> array = (List<Object>)value;
        return buildArray(array);
      }

      if (value instanceof InputStream)
      {
        return buildStream((InputStream)value);
      }

      throw new IllegalArgumentException("Invalid value: " + value);
    }

    public InputStream buildObject(Map<String, Object> object)
    {
      Vector<InputStream> streams = new Vector<InputStream>(2 * object.size() + 1);
      streams.add(IOUtil.streamUTF("{"));

      for (Map.Entry<String, Object> entry : object.entrySet())
      {
        if (streams.size() > 1)
        {
          streams.add(IOUtil.streamUTF(","));
        }

        streams.add(IOUtil.streamUTF("\"" + entry.getKey() + "\":"));
        streams.add(buildValue(entry.getValue()));
      }

      streams.add(IOUtil.streamUTF("}"));
      return new SequenceInputStream(streams.elements());
    }

    public InputStream buildArray(List<Object> array)
    {
      Vector<InputStream> streams = new Vector<InputStream>(2 * array.size() + 1);
      streams.add(IOUtil.streamUTF("["));

      for (Object value : array)
      {
        if (streams.size() > 1)
        {
          streams.add(IOUtil.streamUTF(","));
        }

        streams.add(buildValue(value));
      }

      streams.add(IOUtil.streamUTF("]"));
      return new SequenceInputStream(streams.elements());
    }

    public InputStream buildStream(InputStream stream)
    {
      Vector<InputStream> streams = new Vector<InputStream>(3);
      streams.add(IOUtil.streamUTF("\""));
      streams.add(new Base64InputStream(stream, true, Integer.MAX_VALUE, null));
      streams.add(IOUtil.streamUTF("\""));
      return new SequenceInputStream(streams.elements());
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
  }

  /**
   * @author Eike Stepper
   */
  private static final class JSONParser
  {
    private static final char[] VALUE_CHARS = { '"', '-', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '{', '[', 'n', 't', 'f' };

    private static final char[] NUMBER_CHARS = { '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', 'e', 'E' };

    private static final char[] CONTROL_CHARS = { '"', '\\', '/', 'b', 'f', 'n', 'r', 't', 'u' };

    private final String streamKey;

    private boolean streamAdded;

    private Reader reader;

    private int lookAhead = -1;

    private int pos;

    public JSONParser(InputStream in, String streamKey)
    {
      this.streamKey = streamKey;

      try
      {
        reader = new InputStreamReader(in, StringUtil.UTF8);
      }
      catch (UnsupportedEncodingException ex)
      {
        throw new RuntimeException(ex);
      }
    }

    public Object parseValue() throws IOException
    {
      char c = skipWhitespace();

      if (c == '"')
      {
        return parseString(true);
      }

      if (c == '-' || c >= '0' && c <= '9')
      {
        return parseNumber(c);
      }

      if (c == '{')
      {
        return parseObject();
      }

      if (c == '[')
      {
        return parseArray();
      }

      if (c == 'n')
      {
        expectChar(readChar(), 'u');
        expectChar(readChar(), 'l');
        expectChar(readChar(), 'l');
        return null;
      }

      if (c == 't')
      {
        expectChar(readChar(), 'r');
        expectChar(readChar(), 'u');
        expectChar(readChar(), 'e');
        return true;
      }

      if (c == 'f')
      {
        expectChar(readChar(), 'a');
        expectChar(readChar(), 'l');
        expectChar(readChar(), 's');
        expectChar(readChar(), 'e');
        return false;
      }

      expectChar(c, VALUE_CHARS);
      return null; // Not reachable.
    }

    public String parseString(boolean unescape) throws IOException
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

    public Object parseNumber(char c) throws IOException
    {
      StringBuilder builder = new StringBuilder();
      builder.append(c);

      while (isNumberChar(c = readChar()))
      {
        builder.append(c);
      }

      rememberChar(c);
      return Integer.parseInt(builder.toString());
    }

    public Map<String, Object> parseObject() throws IOException
    {
      Map<String, Object> object = new LinkedHashMap<String, Object>();

      for (;;)
      {
        char c = skipWhitespace();
        if (c == '}')
        {
          return object;
        }

        if (c == ',')
        {
          c = skipWhitespace();
        }

        if (c == '"')
        {
          String key = parseString(false);

          c = skipWhitespace();
          expectChar(c, ':');

          if (key.equals(streamKey))
          {
            c = skipWhitespace();
            expectChar(c, '"');

            InputStream stream = new Base64InputStream(new ValueInputStream());
            object.put(key, stream);

            streamAdded = true;
            return object;
          }
          else
          {
            Object value = parseValue();
            object.put(key, value);

            if (streamAdded)
            {
              return object;
            }
          }
        }
      }
    }

    public List<Object> parseArray() throws IOException
    {
      List<Object> array = new ArrayList<Object>();

      for (;;)
      {
        char c = skipWhitespace();
        if (c == ']')
        {
          return array;
        }

        if (c != ',')
        {
          rememberChar(c);
        }

        Object value = parseValue();
        array.add(value);

        if (streamAdded)
        {
          return array;
        }
      }
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

    private char skipWhitespace() throws IOException
    {
      char c = readChar();
      while (Character.isWhitespace(c))
      {
        c = readChar();
      }

      return c;
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
  public static final class JSONParseException extends IOException
  {
    private static final long serialVersionUID = 1L;

    public JSONParseException(String message)
    {
      super(message);
    }
  }
}
