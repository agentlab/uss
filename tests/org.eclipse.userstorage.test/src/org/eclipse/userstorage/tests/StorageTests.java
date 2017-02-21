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
import org.eclipse.userstorage.internal.StorageService;
import org.eclipse.userstorage.internal.StorageServiceRegistry;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.spi.Credentials;
import org.eclipse.userstorage.tests.util.ClientFixture;
import org.eclipse.userstorage.tests.util.ClientFixture.TestCache;
import org.eclipse.userstorage.tests.util.FixedCredentialsProvider;
import org.eclipse.userstorage.tests.util.ServerFixture;
import org.eclipse.userstorage.tests.util.ServerFixture.BlobInfo;
import org.eclipse.userstorage.tests.util.USSServer;
import org.eclipse.userstorage.util.BadApplicationTokenException;
import org.eclipse.userstorage.util.BadKeyException;
import org.eclipse.userstorage.util.ConflictException;
import org.eclipse.userstorage.util.NotFoundException;
import org.eclipse.userstorage.util.ProtocolException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Eike Stepper
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class StorageTests extends AbstractTest
{
  /**
   * The test application's token.
   */
  public static final String APPLICATION_TOKEN = "pDKTqBfDuNxlAKydhEwxBZPxa4q";

  private static final String KEY = "test_blob";

  private static final String WRONG_ETAG = "wrong_etag";

  private ServerFixture serverFixture;

  private ClientFixture clientFixture;

  private StorageFactory factory;

  private TestCache cache;

  @Override
  public void setUp() throws Exception
  {
    super.setUp();
    StorageServiceRegistry.INSTANCE.stop();
    StorageServiceRegistry.INSTANCE.start();

    serverFixture = new ServerFixture(APPLICATION_TOKEN);
    clientFixture = new ClientFixture(serverFixture);
    factory = clientFixture.getFactory();
    cache = clientFixture.getCache();
  }

  @Override
  public void tearDown() throws Exception
  {
    if (clientFixture != null)
    {
      clientFixture.dispose();
      clientFixture = null;
    }

    if (serverFixture != null)
    {
      serverFixture.dispose();
      serverFixture = null;
    }

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
  public void testCreate() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    String value = "A short UTF-8 string value";
    assertThat(blob.setContentsUTF(value), is(true));

    BlobInfo blobInfo = serverFixture.readServer(blob);
    assertThat(blobInfo.contents, is(value));
    assertThat(blobInfo.eTag, is(blob.getETag()));
  }

  @Test
  public void testCreateWithWrongETag() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);
    blob.setETag(WRONG_ETAG);
    blob.setContentsUTF("Text 2"); // Update attempt 1
    assertThat(blob.getETag(), isNotNull());
  }

  @Test
  public void testCreateAfterRetrieve() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    try
    {
      blob.getContentsUTF();
      fail("NotFoundException expected");
    }
    catch (NotFoundException expected)
    {
      // SUCCESS
    }

    String value = "A short UTF-8 string value";
    blob.setContentsUTF(value);

    BlobInfo blobInfo = serverFixture.readServer(blob);
    assertThat(blobInfo.contents, is(value));
    assertThat(blobInfo.eTag, is(blob.getETag()));
    assertThat(blobInfo.eTag, is(not(Session.NOT_FOUND_ETAG)));
  }

  @Test
  public void testCreateWithCache() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN, cache);
    IBlob blob = storage.getBlob(KEY);

    String value = "A short UTF-8 string value";
    assertThat(blob.setContentsUTF(value), is(true));
    assertThat(clientFixture.readCache(blob.getKey(), null), is(value));
    assertThat(clientFixture.readCache(blob.getKey(), ".properties"), containsString("etag=" + blob.getETag()));
  }

  @Test
  public void testCreateAndUpdate() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    blob.setContentsUTF("Text 1"); // Create
    blob.setContentsUTF("Text 2"); // Update 1
    blob.setContentsUTF("Text 3"); // Update 2
  }

  @Test
  public void testUpdateWithWrongETag() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    blob.setContentsUTF("Text 1"); // Create
    blob.setETag(WRONG_ETAG);

    try
    {
      blob.setContentsUTF("Text 2"); // Update attempt 1
      fail("ConflictException expected");
    }
    catch (ConflictException expected)
    {
      assertThat(expected.getETag(), isNotNull());
      assertThat(blob.getETag(), isNull());
    }
  }

  @Test
  public void testUpdateFailEarly() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    String value1 = "A short UTF-8 string value";
    assertThat(blob.setContentsUTF(value1), is(true));

    File tempFile = File.createTempFile("test-", ".txt");
    IOUtil.writeUTF(tempFile, "Another string value");

    FileInputStream in = null;

    try
    {
      Credentials credentials = new Credentials("abc", "wrong");
      Credentials oldCredentials = FixedCredentialsProvider.setCredentials(credentials);

      try
      {
        ((StorageService)storage.getService()).setCredentials(credentials);

        in = new FileInputStream(tempFile);
        blob.setContents(in);
        fail("ProtocolException: HTTP/1.1 401 Unauthorized expected");
      }
      catch (ProtocolException expected)
      {
        assertThat(expected.getStatusCode(), is(401));
      }
      finally
      {
        FixedCredentialsProvider.setCredentials(oldCredentials);
      }

      assertThat(tempFile.delete(), is(true));
    }
    finally
    {
      IOUtil.closeSilent(in);

      if (!tempFile.delete())
      {
        tempFile.deleteOnExit();
      }
    }
  }

  @Test
  public void testRetrieveNotExistent() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob("aaaaaaaaaa");
    blob.setETag(WRONG_ETAG);

    try
    {
      blob.getContents();
      fail("NotFoundException expected");
    }
    catch (NotFoundException expected)
    {
      // SUCCESS
    }

    assertThat(blob.getETag(), isNull());
  }

  @Test
  public void testRetrieve() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    String value = "A short UTF-8 string value";
    blob.setContentsUTF(value);

    assertThat(blob.getContentsUTF(), is(value));

    BlobInfo blobInfo = serverFixture.readServer(blob);
    assertThat(blobInfo.contents, is(value));
    assertThat(blobInfo.eTag, is(blob.getETag()));
  }

  @Test
  public void testRetrieveWithCache() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN, cache);
    IBlob blob = storage.getBlob(KEY);

    String value = "A short UTF-8 string value";
    blob.setContentsUTF(value);

    assertThat(blob.getContentsUTF(), is(value));
    assertThat(clientFixture.readCache(blob.getKey(), null), is(value));
    assertThat(clientFixture.readCache(blob.getKey(), ".properties"), containsString("etag=" + blob.getETag()));

    InputStream contents = blob.getContents();
    assertThat(contents, instanceOf(FileInputStream.class));

    IOUtil.copy(contents, new ByteArrayOutputStream());
    assertThat(clientFixture.readCache(blob.getKey(), null), is(value));
    assertThat(clientFixture.readCache(blob.getKey(), ".properties"), containsString("etag=" + blob.getETag()));
  }

  @Test
  public void testRetrieveMulti() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);
    blob.setContentsUTF("A short UTF-8 string value");

    blob.getContents();
    blob.getContents();
    blob.getContents();
    blob.getContents();
    blob.getContents();
  }

  @Test
  public void testRetrieveKeys() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    storage.getBlob("anykey1").setContentsUTF("A short UTF-8 string value");
    storage.getBlob("anykey2").setContentsUTF("A short UTF-8 string value");
    storage.getBlob("anykey3").setContentsUTF("A short UTF-8 string value");
    storage.getBlob("anykey4").setContentsUTF("A short UTF-8 string value");

    Set<String> keys = new HashSet<String>();

    for (IBlob blob : storage.getBlobs())
    {
      System.out.println(blob);
      keys.add(blob.getKey());
    }

    assertThat(keys.contains("anykey1"), is(true));
    assertThat(keys.contains("anykey2"), is(true));
    assertThat(keys.contains("anykey3"), is(true));
    assertThat(keys.contains("anykey4"), is(true));
  }

  @Test
  public void testConflict() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);

    // Prepare the conflict.
    String value2 = "Different content";
    String eTag2 = serverFixture.writeServer(blob, value2);

    String value3 = "And now a conflicting string";

    try
    {
      blob.setContentsUTF(value3);
      fail("ConflictException expected");
    }
    catch (ConflictException expected)
    {
      assertThat(expected.getETag(), isNotNull());
      assertThat(blob.getETag(), isNull());
    }

    BlobInfo blobInfo = serverFixture.readServer(blob);
    assertThat(blobInfo.contents, is(value2));
    assertThat(blobInfo.eTag, is(eTag2));
  }

  @Test
  public void testConflictWithCache() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN, cache);
    IBlob blob = storage.getBlob(KEY);

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);

    // Prepare the conflict.
    serverFixture.writeServer(blob, "Different content");

    String value3 = "And now a conflicting string";

    try
    {
      blob.setContentsUTF(value3);
      fail("ConflictException expected");
    }
    catch (ConflictException expected)
    {
      assertThat(expected.getETag(), isNotNull());
      assertThat(blob.getETag(), isNull());
    }

    try
    {
      clientFixture.readCache(blob.getKey(), null);
      fail("FileNotFoundException expected");
    }
    catch (FileNotFoundException expected)
    {
      // SUCCESS
    }

    try
    {
      clientFixture.readCache(blob.getKey(), ".properties");
      fail("FileNotFoundException expected");
    }
    catch (FileNotFoundException expected)
    {
      // SUCCESS
    }
  }

  @Test
  public void testConflictResolution1() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);

    // Prepare the conflict.
    String value2 = "Different content";
    String eTag2 = serverFixture.writeServer(blob, value2);

    assertThat(blob.getContentsUTF(), is(value2)); // Retrieve the latest server version.
    assertThat(blob.getETag(), is(eTag2));

    String value3 = "And now a non-conflicting string";
    blob.setContentsUTF(value3);

    BlobInfo blobInfo = serverFixture.readServer(blob);
    assertThat(blobInfo.contents, is(value3));
    assertThat(blobInfo.eTag, is(blob.getETag()));
  }

  @Test
  public void testConflictResolution1WithCache() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN, cache);
    IBlob blob = storage.getBlob(KEY);

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);

    // Prepare the conflict.
    String value2 = "Different content";
    String eTag2 = serverFixture.writeServer(blob, value2);

    assertThat(blob.getContentsUTF(), is(value2)); // Retrieve the latest server version.
    assertThat(blob.getETag(), is(eTag2));

    String value3 = "And now a non-conflicting string";
    blob.setContentsUTF(value3);
    assertThat(clientFixture.readCache(blob.getKey(), null), is(value3));
    assertThat(clientFixture.readCache(blob.getKey(), ".properties"), containsString("etag=" + blob.getETag()));
  }

  @Test
  public void testConflictResolution2() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);

    // Prepare the conflict.
    String value2 = "Different content";
    String eTag2 = serverFixture.writeServer(blob, value2);

    String value3 = "And now a conflicting string";

    try
    {
      blob.setContentsUTF(value3);
      fail("ConflictException expected");
    }
    catch (ConflictException expected)
    {
      assertThat(expected.getETag(), isNotNull());
      assertThat(blob.getETag(), isNull());
      blob.setETag(eTag2);
    }

    blob.setContentsUTF(value3);

    BlobInfo blobInfo = serverFixture.readServer(blob);
    assertThat(blobInfo.contents, is(value3));
    assertThat(blobInfo.eTag, is(blob.getETag()));
  }

  @Test
  public void testConflictResolution2WithCache() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN, cache);
    IBlob blob = storage.getBlob(KEY);

    String value1 = "A short UTF-8 string value";
    blob.setContentsUTF(value1);

    // Prepare the conflict.
    String value2 = "Different content";
    String eTag2 = serverFixture.writeServer(blob, value2);

    String value3 = "And now a conflicting string";

    try
    {
      blob.setContentsUTF(value3);
      fail("ConflictException expected");
    }
    catch (ConflictException expected)
    {
      assertThat(expected.getETag(), isNotNull());
      assertThat(blob.getETag(), isNull());

      blob.setETag(eTag2); // Delete cache.
    }

    try
    {
      clientFixture.readCache(blob.getKey(), null);
      fail("FileNotFoundException expected");
    }
    catch (FileNotFoundException expected)
    {
      // SUCCESS
    }

    try
    {
      clientFixture.readCache(blob.getKey(), ".properties");
      fail("FileNotFoundException expected");
    }
    catch (FileNotFoundException expected)
    {
      // SUCCESS
    }

    blob.setContentsUTF(value3);
    assertThat(clientFixture.readCache(blob.getKey(), null), is(value3));
    assertThat(clientFixture.readCache(blob.getKey(), ".properties"), containsString("etag=" + blob.getETag()));
  }

  @Test
  public void testAuthenticateFailure() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    Credentials credentials = new Credentials("abcd", "wrong123");
    Credentials oldCredentials = FixedCredentialsProvider.setCredentials(credentials);

    try
    {
      ((StorageService)storage.getService()).setCredentials(credentials);

      String value1 = "A short UTF-8 string value";
      assertThat(blob.setContentsUTF(value1), is(true));

      fail("ProtocolException: HTTP/1.1 401 Unauthorized expected");
    }
    catch (ProtocolException expected)
    {
      assertThat(expected.getStatusCode(), is(401));
    }
    finally
    {
      FixedCredentialsProvider.setCredentials(oldCredentials);
    }
  }

  @Test
  public void testReauthenticate() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    String value = "A short UTF-8 string value";
    blob.setContentsUTF(value);

    if (serverFixture.hasLocalServer())
    {
      Map<String, USSServer.Session> sessions = serverFixture.getServer().getSessions();
      assertThat(sessions.size(), is(1));
      sessions.clear();
    }

    assertThat(blob.getContentsUTF(), is(value));

    BlobInfo blobInfo = serverFixture.readServer(blob);
    assertThat(blobInfo.contents, is(value));
    assertThat(blobInfo.eTag, is(blob.getETag()));

    if (serverFixture.hasLocalServer())
    {
      Map<String, USSServer.Session> sessions = serverFixture.getServer().getSessions();
      assertThat(sessions.size(), is(1));
    }
  }

  @Test
  public void testDelete() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    String value = "A short UTF-8 string value";
    blob.setContentsUTF(value);
    assertThat(blob.getContentsUTF(), is(value));

    blob.delete();

    BlobInfo blobInfo = serverFixture.readServer(blob);
    assertThat(blobInfo, isNull());

    try
    {
      blob.getContentsUTF();
      fail("NotFoundException expected");
    }
    catch (NotFoundException expected)
    {
      // SUCCESS
    }
  }

  @Test
  public void testDeleteWithoutETag() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    String value = "A short UTF-8 string value";
    blob.setContentsUTF(value);
    assertThat(blob.getContentsUTF(), is(value));

    blob.setETag(null);
    blob.delete();

    BlobInfo blobInfo = serverFixture.readServer(blob);
    assertThat(blobInfo, isNull());

    try
    {
      blob.getContentsUTF();
      fail("NotFoundException expected");
    }
    catch (NotFoundException expected)
    {
      // SUCCESS
    }
  }

  @Test
  public void testDeleteWithCache() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN, cache);
    IBlob blob = storage.getBlob(KEY);

    String value = "A short UTF-8 string value";
    blob.setContentsUTF(value);
    assertThat(blob.getContentsUTF(), is(value));

    blob.delete();

    try
    {
      clientFixture.readCache(blob.getKey(), null);
      fail("FileNotFoundException expected");
    }
    catch (FileNotFoundException expected)
    {
      // SUCCESS
    }

    try
    {
      clientFixture.readCache(blob.getKey(), ".properties");
      fail("FileNotFoundException expected");
    }
    catch (FileNotFoundException expected)
    {
      // SUCCESS
    }
  }

  @Test
  public void testDeleteAll() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    assertThat(storage.getBlob(KEY).setContentsUTF("A short UTF-8 string value"), is(true));

    boolean deleted = storage.deleteAllBlobs();
    assertThat(deleted, is(true));
    assertThat(storage.getBlobs().iterator().hasNext(), is(false));
  }

  @Test
  public void testDeleteNotExistent() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);

    boolean deleted = blob.delete();
    assertThat(deleted, is(false));
  }

  @Test
  public void testDeleteNotExistentWithWrongETag() throws Exception
  {
    IStorage storage = factory.create(APPLICATION_TOKEN);
    IBlob blob = storage.getBlob(KEY);
    blob.setETag(WRONG_ETAG);

    boolean deleted = blob.delete();
    assertThat(deleted, is(false));
  }
}
