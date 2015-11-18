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

import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.IStorage;
import org.eclipse.userstorage.StorageFactory;
import org.eclipse.userstorage.internal.Session;
import org.eclipse.userstorage.tests.util.USSFixture;
import org.eclipse.userstorage.tests.util.USSFixture.BlobInfo;
import org.eclipse.userstorage.tests.util.USSFixture.TestCache;
import org.eclipse.userstorage.util.BadApplicationTokenException;
import org.eclipse.userstorage.util.BadKeyException;
import org.eclipse.userstorage.util.ConflictException;
import org.eclipse.userstorage.util.ProtocolException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.FileNotFoundException;
import java.util.UUID;

/**
 * @author Eike Stepper
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class StorageTests extends AbstractTest
{
  // private static final boolean REMOTE = Boolean.getBoolean(StorageTests.class.getName() + ".remote");
  //
  // private static final File SERVER = new File(System.getProperty("java.io.tmpdir"), "uss-tests/server");
  //
  // private static final File CACHE = new File(System.getProperty("java.io.tmpdir"), "uss-tests/cache");

  private static final String APPLICATION_TOKEN = "pDKTqBfDuNxlAKydhEwxBZPxa4q";

  private static final String KEY = "test_blob";

  private USSFixture fixture;

  // private USSServer server;
  //
  // private User user;
  //
  // private IStorageService.Dynamic service;
  //
  private StorageFactory factory;

  private TestCache cache;

  @Override
  public void setUp() throws Exception
  {
    super.setUp();
    fixture = new USSFixture(APPLICATION_TOKEN);
    factory = fixture.getFactory();
    cache = fixture.getCache();

    // Activator.start();
    //
    // if (REMOTE)
    // {
    // service = IStorageService.Registry.INSTANCE.addService("Eclipse.org (Staging)", StringUtil.newURI("https://api-staging.eclipse.org/"));
    // }
    // else
    // {
    // IOUtil.deleteFiles(SERVER);
    //
    // server = new USSServer(8080, SERVER);
    // user = server.addUser(FixedCredentialsProvider.DEFAULT_CREDENTIALS);
    // server.getApplicationTokens().add(APPLICATION_TOKEN);
    // int port = server.start();
    //
    // service = IStorageService.Registry.INSTANCE.addService("Local", StringUtil.newURI("http://localhost:" + port));
    // }
    //
    // ISettings settings = new MemorySettings(Collections.singletonMap(APPLICATION_TOKEN, service.getServiceURI().toString()));
    // factory = new StorageFactory(settings);
    //
    // IOUtil.deleteFiles(CACHE);
    // cache = new TestCache(CACHE);
  }

  @Override
  public void tearDown() throws Exception
  {
    fixture.dispose();
    super.tearDown();
  }

  @Test
  public void testApplicationToken() throws Exception
  {
    try
    {
      factory.create(null);
      fail("BadApplicationTokenException expected");
    }
    catch (BadApplicationTokenException expected)
    {
      // SUCCESS
    }

    try
    {
      factory.create("aaaa"); // Too short.
      fail("BadApplicationTokenException expected");
    }
    catch (BadApplicationTokenException expected)
    {
      // SUCCESS
    }

    try
    {
      factory.create("aaaaaaaaaaaaaaaaaaaaaaaaaaaa"); // Too long.
      fail("BadApplicationTokenException expected");
    }
    catch (BadApplicationTokenException expected)
    {
      // SUCCESS
    }

    try
    {
      factory.create("1aaaaaaaaa"); // Too long.
      fail("BadApplicationTokenException expected");
    }
    catch (BadApplicationTokenException expected)
    {
      // SUCCESS
    }

    factory.create("aaaaa"); // Just short enough.
    factory.create("aaaaaaaaaaaaaaaaaaaaaaaaaaa"); // Just long enough.
  }

  @Test
  public void testKey() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);

    try
    {
      storage.getBlob(null);
      fail("BadKeyException expected");
    }
    catch (BadKeyException expected)
    {
      // SUCCESS
    }

    try
    {
      storage.getBlob("aaaa"); // Too short.
      fail("BadKeyException expected");
    }
    catch (BadKeyException expected)
    {
      // SUCCESS
    }

    try
    {
      storage.getBlob("aaaaaaaaaaaaaaaaaaaaaaaaaaa"); // Too long.
      fail("BadKeyException expected");
    }
    catch (BadKeyException expected)
    {
      // SUCCESS
    }

    try
    {
      storage.getBlob("1aaaaaaaaa"); // Too long.
      fail("BadKeyException expected");
    }
    catch (BadKeyException expected)
    {
      // SUCCESS
    }

    storage.getBlob("aaaaa"); // Just short enough.
    storage.getBlob("aaaaaaaaaaaaaaaaaaaaaaaaa"); // Just long enough.
  }

  @Test
  public void testUserAgent() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    System.setProperty(Session.USER_AGENT_PROPERTY, "malicious/client");

    try
    {
      IBlob blob = storage.getBlob("any_blob");
      blob.getContents();
      fail("ProtocolException expected");
    }
    catch (ProtocolException expected)
    {
      assertThat(expected.getStatusCode(), is(403)); // Forbidden.
    }
    finally
    {
      System.clearProperty(Session.USER_AGENT_PROPERTY);
    }
  }

  @Test
  public void testUpdate() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(makeKey());

    String value = "A short UTF-8 string value";
    assertThat(blob.setContentsUTF(value), is(true));

    BlobInfo blobInfo = fixture.readServer(blob);
    assertThat(blobInfo.contents, is(value));
    assertThat(blobInfo.eTag, is(blob.getETag()));
  }

  @Test
  public void testUpdateWithCache() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN, cache);
    IBlob blob = storage.getBlob(makeKey());

    String value = "A short UTF-8 string value";
    assertThat(blob.setContentsUTF(value), is(true));
    assertThat(fixture.readCache(blob.getKey(), null), is(value));
    assertThat(fixture.readCache(blob.getKey(), ".properties"), containsString("etag=" + blob.getETag()));
  }

  @Test
  public void testUpdateMulti() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(makeKey());

    blob.setContentsUTF("Text 1");
    blob.setContentsUTF("Text 2");
    blob.setContentsUTF("Text 3");
  }

  @Test
  public void testRetrieveNotFound() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob("aaaaaaaaaa");
    assertThat(blob.getContents(), isNull());
  }

  @Test
  public void testRetrieve() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(makeKey());

    String value = "A short UTF-8 string value";
    blob.setContentsUTF(value);

    assertThat(blob.getContentsUTF(), is(value));

    BlobInfo blobInfo = fixture.readServer(blob);
    assertThat(blobInfo.contents, is(value));
    assertThat(blobInfo.eTag, is(blob.getETag()));
  }

  @Test
  public void testRetrieveWithCache() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN, cache);
    IBlob blob = storage.getBlob(makeKey());

    String value = "A short UTF-8 string value";
    blob.setContentsUTF(value);

    assertThat(blob.getContentsUTF(), is(value));
    assertThat(fixture.readCache(blob.getKey(), null), is(value));
    assertThat(fixture.readCache(blob.getKey(), ".properties"), containsString("etag=" + blob.getETag()));
  }

  @Test
  public void testRetrieveMulti() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(makeKey());
    blob.setContentsUTF("A short UTF-8 string value");

    blob.getContents();
    blob.getContents();
    blob.getContents();
    blob.getContents();
    blob.getContents();
  }

  @Test
  public void testConflict() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(makeKey());

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);
    String eTag1 = blob.getETag();

    // Prepare the conflict.
    String value2 = "Different content";
    String eTag2 = fixture.writeServer(blob, value2);

    String value3 = "And now a conflicting string";

    try
    {
      blob.setContentsUTF(value3);
      fail("ConflictException expected");
    }
    catch (ConflictException expected)
    {
      assertThat(expected.getStatusCode(), is(409)); // Conflict.
      assertThat(expected.getETag(), isNull());
    }

    assertThat(blob.getETag(), is(eTag1));

    BlobInfo blobInfo = fixture.readServer(blob);
    assertThat(blobInfo.contents, is(value2));
    assertThat(blobInfo.eTag, is(eTag2));
  }

  @Test
  public void testConflictWithCache() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN, cache);
    IBlob blob = storage.getBlob(makeKey());

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);
    String eTag1 = blob.getETag();

    // Prepare the conflict.
    fixture.writeServer(blob, "Different content");

    String value3 = "And now a conflicting string";

    try
    {
      blob.setContentsUTF(value3);
      fail("ConflictException expected");
    }
    catch (ConflictException expected)
    {
      assertThat(expected.getStatusCode(), is(409)); // Conflict.
      assertThat(expected.getETag(), isNull());
    }

    // It's okay for the cache to have the new value. The old ETag (see below) will cause cache refresh...
    assertThat(fixture.readCache(blob.getKey(), null), is(value3));

    // Cache and blob ETags must still be in old state.
    assertThat(fixture.readCache(blob.getKey(), ".properties"), containsString("etag=" + eTag1));
    assertThat(blob.getETag(), is(eTag1));
  }

  @Test
  public void testConflictResolution1() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(makeKey());

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);

    // Prepare the conflict.
    String value2 = "Different content";
    String eTag2 = fixture.writeServer(blob, value2);

    assertThat(blob.getContentsUTF(), is(value2));
    assertThat(blob.getETag(), is(eTag2));

    String value3 = "And now a non-conflicting string";
    blob.setContentsUTF(value3);

    BlobInfo blobInfo = fixture.readServer(blob);
    assertThat(blobInfo.contents, is(value3));
    assertThat(blobInfo.eTag, is(blob.getETag()));
  }

  @Test
  public void testConflictResolution1WithCache() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN, cache);
    IBlob blob = storage.getBlob(makeKey());

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);

    // Prepare the conflict.
    String value2 = "Different content";
    String eTag2 = fixture.writeServer(blob, value2);

    assertThat(blob.getContentsUTF(), is(value2));
    assertThat(blob.getETag(), is(eTag2));

    String value3 = "And now a non-conflicting string";
    blob.setContentsUTF(value3);
    assertThat(fixture.readCache(blob.getKey(), null), is(value3));
    assertThat(fixture.readCache(blob.getKey(), ".properties"), containsString("etag=" + blob.getETag()));
  }

  @Test
  public void testConflictResolution2() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(makeKey());

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);

    // Prepare the conflict.
    String value2 = "Different content";
    String eTag2 = fixture.writeServer(blob, value2);

    String value3 = "And now a conflicting string";

    try
    {
      blob.setContentsUTF(value3);
      fail("ConflictException expected");
    }
    catch (ConflictException expected)
    {
      blob.setETag(eTag2);
    }

    blob.setContentsUTF(value3);

    BlobInfo blobInfo = fixture.readServer(blob);
    assertThat(blobInfo.contents, is(value3));
    assertThat(blobInfo.eTag, is(blob.getETag()));
  }

  @Test
  public void testConflictResolution2WithCache() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN, cache);
    IBlob blob = storage.getBlob(makeKey());

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);

    // Prepare the conflict.
    String value2 = "Different content";
    String eTag2 = fixture.writeServer(blob, value2);

    String value3 = "And now a conflicting string";

    try
    {
      blob.setContentsUTF(value3);
      fail("ConflictException expected");
    }
    catch (ConflictException expected)
    {
      blob.setETag(eTag2); // Delete cache.

      try
      {
        fixture.readCache(blob.getKey(), null);
        fail("FileNotFoundException expected");
      }
      catch (FileNotFoundException expected2)
      {
        // SUCCESS
      }

      try
      {
        fixture.readCache(blob.getKey(), ".properties");
        fail("FileNotFoundException expected");
      }
      catch (FileNotFoundException expected2)
      {
        // SUCCESS
      }
    }

    blob.setContentsUTF(value3);
    assertThat(fixture.readCache(blob.getKey(), null), is(value3));
    assertThat(fixture.readCache(blob.getKey(), ".properties"), containsString("etag=" + blob.getETag()));
  }

  @Test
  public void testReauthenticate() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(makeKey());

    String value = "A short UTF-8 string value";
    blob.setContentsUTF(value);

    if (fixture.hasLocalServer())
    {
      assertThat(fixture.getServer().getSessions().size(), is(1));
      fixture.getServer().getSessions().clear();
    }

    assertThat(blob.getContentsUTF(), is(value));

    BlobInfo blobInfo = fixture.readServer(blob);
    assertThat(blobInfo.contents, is(value));
    assertThat(blobInfo.eTag, is(blob.getETag()));

    if (fixture.hasLocalServer())
    {
      assertThat(fixture.getServer().getSessions().size(), is(1));
    }
  }

  private String makeKey()
  {
    if (fixture.hasLocalServer())
    {
      return KEY;
    }

    StringBuilder builder = new StringBuilder("T" + UUID.randomUUID().toString());
    for (int i = 0; i < builder.length(); i++)
    {
      if (i == 25)
      {
        builder.setLength(25);
        break;
      }

      char c = builder.charAt(i);
      if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_'))
      {
        builder.replace(i, i + 1, "");
        --i;
      }
    }

    return builder.toString();
  }
}
