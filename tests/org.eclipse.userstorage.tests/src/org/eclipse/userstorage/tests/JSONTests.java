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

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eike Stepper
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class JSONTests extends AbstractTest
{
  @Test
  public void testBuildObject() throws Exception
  {
    Map<String, Object> object = new LinkedHashMap<String, Object>();
    object.put("blob_key", "user_xml");
    object.put("blob_namespace", "org_eclipse_userstorage");
    object.put("blob_hash", "8e6706262c374adacd1048c5497e03cb4c5ea585c07d5e36c15150d4f7a40812");
    object.put("created", 1445368225);
    object.put("changed", 1445368225);
    object.put("blob_value", new FileInputStream("about.html"));

    InputStream json = JSONUtil.build(object);
    dump(json);
    IOUtil.close(json);
  }

  @Test
  public void testParseObject() throws Exception
  {
    String response = "{\"blob_key\":\"user_xml\"," //
        + "\"blob_namespace\":\"org_eclipse_userstorage\"," //
        + "\"blob_hash\":\"8e6706262c374adacd1048c5497e03cb4c5ea585c07d5e36c15150d4f7a40812\"," //
        + "\"created\":\"1445368225\"," //
        + "\"changed\":\"1445368225\"," //
        + "\"blob_value\":\"PHhtbD48cGFja2FnZSBuYW1lPSJFY2xpcHNlIENsYXNzaWMgMy42LjAiIGRvd25sb2FkQ291bnQ9IjYwNzY2NCIgdXJsPSIvZG93bmxvYWRzL3BhY2thZ2VzL2VjbGlwc2UtY2xhc3NpYy0zNjAvaGVsaW9zciIgZG93bmxvYWR1cmw9Imh0dHA6Ly93d3cuZWNsaXBzZS5vcmcvZG93bmxvYWRzL2Rvd25sb2FkLnBocD9maWxlPS9lY2xpcHNlL2Rvd25sb2Fkcy9kcm9wcy9SLTMuNi0yMDEwMDYwODA5MTEvZWNsaXBzZS1TREstMy42LW1hY29zeC1jYXJib24udGFyLmd6IiBkb3dubG9hZHVybDY0PSIiIHNpemU9IjE2OSBNQiIgaWNvbj0iaHR0cDovL3d3dy5lY2xpcHNlLm9yZy9kb3dubG9hZHMvaW1hZ2VzL2NsYXNzaWMyLmpwZyIvPiAKPC94bWw+\"}";

    Map<String, Object> object = JSONUtil.parse(IOUtil.streamUTF(response), "blob_value");
    JSONUtil.dump(object);

    System.out.println();
    InputStream stream = (InputStream)object.get("blob_value");
    dump(stream);
  }

  @Test
  public void testBuildParse() throws Exception
  {
    Map<String, Object> objectIn = new LinkedHashMap<String, Object>();
    objectIn.put("blob_key", "user_xml");
    objectIn.put("blob_namespace", "org_eclipse_userstorage");
    objectIn.put("blob_hash", "8e6706262c374adacd1048c5497e03cb4c5ea585c07d5e36c15150d4f7a40812");
    objectIn.put("created", 1445368225);
    objectIn.put("changed", 1445368225);
    objectIn.put("blob_value", new FileInputStream("about.html"));

    InputStream json = JSONUtil.build(objectIn);

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    copy(json, buffer);
    IOUtil.close(json);

    Map<String, Object> objectOut = JSONUtil.parse(IOUtil.streamUTF(buffer.toByteArray()), "blob_value");
    JSONUtil.dump(objectOut);
    System.out.println();

    InputStream stream = (InputStream)objectOut.get("blob_value");
    dump(stream);
  }

  @Test
  public void testParseArray() throws Exception
  {
    String response = "[{\"key\":\"user_xml\",\"application_token\":\"org_eclipse_oomph\",\"etag\":\"8e6706262c374adacd1048c5497e03cb4c5ea585c07d5e36c15150d4f7a40812\",\"created\":1445368225,\"changed\":1445368225, \"url\":\"https://api.eclipse.org/api/blob/:namespace/:key\" }"
        + ",{\"key\":\"pref_xml\",\"application_token\":\"org_eclipse_oomph\",\"etag\":\"8e6706262c374adacd1048c5497e03cb4c5ea585c07d5e36c15150d4f7a40812\",\"created\":1445368264,\"changed\":1445368264, \"url\":\"https://api.eclipse.org/api/blob/:namespace/:key\" }]";

    List<Object> array = JSONUtil.parse(IOUtil.streamUTF(response), null);
    JSONUtil.dump(array);
  }

  @Test
  public void testParseArray2() throws Exception
  {
    String objectJSON = "{\"blob_key\":\"user_xml\"," //
        + "\"blob_namespace\":\"org_eclipse_userstorage\"," //
        + "\"blob_hash\":\"8e6706262c374adacd1048c5497e03cb4c5ea585c07d5e36c15150d4f7a40812\"," //
        + "\"created\":\"1445368225\"," //
        + "\"changed\":\"1445368225\"," //
        + "\"blob_value\":\"PHhtbD48cGFja2FnZSBuYW1lPSJFY2xpcHNlIENsYXNzaWMgMy42LjAiIGRvd25sb2FkQ291bnQ9IjYwNzY2NCIgdXJsPSIvZG93bmxvYWRzL3BhY2thZ2VzL2VjbGlwc2UtY2xhc3NpYy0zNjAvaGVsaW9zciIgZG93bmxvYWR1cmw9Imh0dHA6Ly93d3cuZWNsaXBzZS5vcmcvZG93bmxvYWRzL2Rvd25sb2FkLnBocD9maWxlPS9lY2xpcHNlL2Rvd25sb2Fkcy9kcm9wcy9SLTMuNi0yMDEwMDYwODA5MTEvZWNsaXBzZS1TREstMy42LW1hY29zeC1jYXJib24udGFyLmd6IiBkb3dubG9hZHVybDY0PSIiIHNpemU9IjE2OSBNQiIgaWNvbj0iaHR0cDovL3d3dy5lY2xpcHNlLm9yZy9kb3dubG9hZHMvaW1hZ2VzL2NsYXNzaWMyLmpwZyIvPiAKPC94bWw+\"}";

    String arrayJSON = "[" + objectJSON + ", true, false, null, [1,2,3,4,5,6,7,8,9], " + objectJSON + "]";

    Object array = JSONUtil.parse(IOUtil.streamUTF(arrayJSON), "blob_value");
    JSONUtil.dump(array);
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
