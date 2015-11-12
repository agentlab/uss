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
package org.eclipse.userstorage.tests;

import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.JSONUtil;
import org.eclipse.userstorage.internal.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public final class JSONTests
{
  public static void main(String[] args) throws Exception
  {
    // testEncode();
    // testDecode();
    testEncodeDecode();
  }

  public static void testEncode() throws Exception
  {
    Map<String, String> properties = new LinkedHashMap<String, String>();
    properties.put("blob_key", "user_xml");
    properties.put("blob_namespace", "org_eclipse_userstorage");
    properties.put("blob_hash", "8e6706262c374adacd1048c5497e03cb4c5ea585c07d5e36c15150d4f7a40812");
    properties.put("created", "1445368225");
    properties.put("changed", "1445368225");

    InputStream stream = JSONUtil.encode(properties, "blob_value", new FileInputStream("about.html"));
    dump(stream);
    IOUtil.close(stream);
  }

  public static void testDecode() throws IOException
  {
    String response = "{\"blob_key\":\"user_xml\"," //
        + "\"blob_namespace\":\"org_eclipse_userstorage\"," //
        + "\"blob_hash\":\"8e6706262c374adacd1048c5497e03cb4c5ea585c07d5e36c15150d4f7a40812\"," //
        + "\"created\":\"1445368225\"," //
        + "\"changed\":\"1445368225\"," //
        + "\"blob_value\":\"PHhtbD48cGFja2FnZSBuYW1lPSJFY2xpcHNlIENsYXNzaWMgMy42LjAiIGRvd25sb2FkQ291bnQ9IjYwNzY2NCIgdXJsPSIvZG93bmxvYWRzL3BhY2thZ2VzL2VjbGlwc2UtY2xhc3NpYy0zNjAvaGVsaW9zciIgZG93bmxvYWR1cmw9Imh0dHA6Ly93d3cuZWNsaXBzZS5vcmcvZG93bmxvYWRzL2Rvd25sb2FkLnBocD9maWxlPS9lY2xpcHNlL2Rvd25sb2Fkcy9kcm9wcy9SLTMuNi0yMDEwMDYwODA5MTEvZWNsaXBzZS1TREstMy42LW1hY29zeC1jYXJib24udGFyLmd6IiBkb3dubG9hZHVybDY0PSIiIHNpemU9IjE2OSBNQiIgaWNvbj0iaHR0cDovL3d3dy5lY2xpcHNlLm9yZy9kb3dubG9hZHMvaW1hZ2VzL2NsYXNzaWMyLmpwZyIvPiAKPC94bWw+\"}";

    Map<String, String> properties = new LinkedHashMap<String, String>();
    InputStream stream = JSONUtil.decode(new ByteArrayInputStream(StringUtil.toUTF(response)), properties, "blob_value");

    dump(properties);
    System.out.println();
    dump(stream);
  }

  public static void testEncodeDecode() throws IOException
  {
    Map<String, String> properties = new LinkedHashMap<String, String>();
    properties.put("blob_key", "user_xml");
    properties.put("blob_namespace", "org_eclipse_userstorage");
    properties.put("blob_hash", "8e6706262c374adacd1048c5497e03cb4c5ea585c07d5e36c15150d4f7a40812");
    properties.put("created", "1445368225");
    properties.put("changed", "1445368225");

    InputStream source = JSONUtil.encode(properties, "blob_value", new FileInputStream("about.html"));

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    copy(source, buffer);
    IOUtil.close(source);

    properties.clear();
    InputStream stream = JSONUtil.decode(new ByteArrayInputStream(buffer.toByteArray()), properties, "blob_value");

    dump(properties);
    System.out.println();
    dump(stream);
  }

  private static void dump(Map<String, String> properties)
  {
    for (Map.Entry<String, String> entry : properties.entrySet())
    {
      System.out.println(entry.getKey() + " = " + entry.getValue());
    }
  }

  private static void dump(InputStream stream) throws IOException
  {
    int c;
    while ((c = stream.read()) != -1)
    {
      System.out.print((char)c);
    }

    System.out.println();
  }

  private static void copy(InputStream source, OutputStream target) throws IOException
  {
    int c;
    while ((c = source.read()) != -1)
    {
      target.write(c);
    }
  }
}
