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
package org.eclipse.userstorage.internal;

import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.spi.ICredentialsProvider;
import org.eclipse.userstorage.util.ConflictException;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * @author Eike Stepper
 */
public class StorageService implements IStorageService
{
  private static final String USERNAME_KEY = "username";

  private static final String PASSWORD_KEY = "password";

  private final Semaphore authenticationSemaphore = new Semaphore(1);

  private final String serviceLabel;

  private final URI serviceURI;

  private final URI createAccountURI;

  private final URI editAccountURI;

  private final URI recoverPasswordURI;

  private ICredentialsProvider credentialsProvider;

  private Session session;

  public StorageService(String serviceLabel, URI serviceURI, URI createAccountURI, URI editAccountURI, URI recoverPasswordURI)
  {
    if (StringUtil.isEmpty(serviceLabel))
    {
      throw new IllegalArgumentException("Service label is null or empty");
    }

    if (serviceURI == null)
    {
      throw new IllegalArgumentException("Service URI is null");
    }

    this.serviceLabel = serviceLabel;
    this.serviceURI = serviceURI;
    this.createAccountURI = createAccountURI;
    this.editAccountURI = editAccountURI;
    this.recoverPasswordURI = recoverPasswordURI;
  }

  @Override
  public String getServiceLabel()
  {
    return serviceLabel;
  }

  @Override
  public URI getServiceURI()
  {
    return serviceURI;
  }

  @Override
  public URI getCreateAccountURI()
  {
    return createAccountURI;
  }

  @Override
  public URI getEditAccountURI()
  {
    return editAccountURI;
  }

  @Override
  public URI getRecoverPasswordURI()
  {
    return recoverPasswordURI;
  }

  @Override
  public Semaphore getAuthenticationSemaphore()
  {
    return authenticationSemaphore;
  }

  public Credentials getCredentials()
  {
    try
    {
      ISecurePreferences securePreferences = getSecurePreferences();
      if (securePreferences != null)
      {
        String username = securePreferences.get(USERNAME_KEY, null);
        String password = securePreferences.get(PASSWORD_KEY, null);

        if (username != null && password != null)
        {
          return new Credentials(username, password);
        }
      }
    }
    catch (StorageException ex)
    {
      Activator.log(ex);
    }

    return null;
  }

  public void setCredentials(Credentials credentials)
  {
    try
    {
      ISecurePreferences securePreferences = getSecurePreferences();
      if (securePreferences != null)
      {
        if (credentials == null)
        {
          securePreferences.remove(USERNAME_KEY);
          securePreferences.remove(PASSWORD_KEY);
        }
        else
        {
          securePreferences.put(USERNAME_KEY, credentials.getUsername(), true);
          securePreferences.put(PASSWORD_KEY, credentials.getPassword(), true);
        }

        securePreferences.flush();
      }
    }
    catch (Exception ex)
    {
      Activator.log(ex);
    }
    finally
    {
      if (session != null)
      {
        session.reset();
      }
    }
  }

  public synchronized InputStream retrieveBlob(ICredentialsProvider credentialsProvider, String appToken, String key, Map<String, String> properties,
      boolean useETag) throws IOException
  {
    if (credentialsProvider == null)
    {
      credentialsProvider = getCredentialsProvider();
    }

    Session session = getSession();
    return session.retrieveBlob(appToken, key, properties, useETag, credentialsProvider);
  }

  public synchronized boolean updateBlob(ICredentialsProvider credentialsProvider, String appToken, String key, Map<String, String> properties, InputStream in)
      throws IOException, ConflictException
  {
    if (credentialsProvider == null)
    {
      credentialsProvider = getCredentialsProvider();
    }

    Session session = getSession();
    return session.updateBlob(appToken, key, properties, in, credentialsProvider);
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (serviceURI == null ? 0 : serviceURI.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }

    if (obj == null)
    {
      return false;
    }

    if (getClass() != obj.getClass())
    {
      return false;
    }

    StorageService other = (StorageService)obj;
    if (serviceURI == null)
    {
      if (other.serviceURI != null)
      {
        return false;
      }
    }
    else if (!serviceURI.equals(other.serviceURI))
    {
      return false;
    }

    return true;
  }

  @Override
  public int compareTo(IStorageService o)
  {
    return serviceLabel.compareTo(o.getServiceLabel());
  }

  @Override
  public String toString()
  {
    return serviceLabel;
  }

  public ISecurePreferences getSecurePreferences()
  {
    ISecurePreferences securePreferences = Activator.getSecurePreferences();
    if (securePreferences != null)
    {
      String serviceNode = StringUtil.encodeURI(serviceURI);
      return securePreferences.node(serviceNode);
    }

    return null;
  }

  private ICredentialsProvider getCredentialsProvider()
  {
    if (credentialsProvider == null)
    {
      String property = System.getProperty(StorageProperties.CREDENTIALS_PROVIDER, null);
      if (property != null)
      {
        try
        {
          @SuppressWarnings("unchecked")
          Class<ICredentialsProvider> c = (Class<ICredentialsProvider>)Class.forName(property);
          credentialsProvider = c.newInstance();
        }
        catch (Throwable ex)
        {
          Activator.log(ex);
        }
      }

      if (credentialsProvider == null)
      {
        credentialsProvider = Activator.getCredentialsProvider();
      }
    }

    return credentialsProvider;
  }

  private Session getSession()
  {
    if (session == null)
    {
      session = openSession();
    }

    return session;
  }

  private Session openSession()
  {
    return new Session(this);
  }

  /**
   * @author Eike Stepper
   */
  public static final class DynamicService extends StorageService implements IStorageService.Dynamic
  {
    public DynamicService(String serviceLabel, URI serviceURI, URI createAccountURI, URI editAccountURI, URI recoverPasswordURI)
    {
      super(serviceLabel, serviceURI, createAccountURI, editAccountURI, recoverPasswordURI);
    }

    @Override
    public void remove()
    {
      StorageServiceRegistry.INSTANCE.removeService(this);

      ISecurePreferences securePreferences = getSecurePreferences();
      if (securePreferences != null)
      {
        try
        {
          ISecurePreferences parent = securePreferences.parent();
          securePreferences.removeNode();
          parent.flush();
        }
        catch (Exception ex)
        {
          Activator.log(ex);
        }
      }
    }
  }
}
