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
package org.eclipse.userstorage.tests.util;

import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.spi.Credentials;
import org.eclipse.userstorage.spi.ICredentialsProvider;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * @author Eike Stepper
 */
public class TestCredentialsProvider implements ICredentialsProvider
{
  private static final File CREDENTIALS_FILE = new File(System.getProperty("userstorage.credentials.file", "userstorage.credentials"));

  public TestCredentialsProvider()
  {
  }

  @Override
  public Credentials provideCredentials(IStorageService service, boolean reauthentication)
  {
    if (CREDENTIALS_FILE.exists())
    {
      System.out.println("Reading credentials from file " + CREDENTIALS_FILE);
      System.out.println();

      return (Credentials)IOUtil.readObject(CREDENTIALS_FILE);
    }

    Credentials credentials = new LoginDialog().getCredentials();
    if (credentials == null)
    {
      throw new RuntimeException("No credentials entered!");
    }

    System.out.println("Storing credentials in file " + CREDENTIALS_FILE);
    System.out.println();

    IOUtil.writeObject(CREDENTIALS_FILE, credentials);

    return credentials;
  }

  /**
   * @author Eike Stepper
   */
  private static class LoginDialog extends JDialog
  {
    private static final long serialVersionUID = 1L;

    private Credentials credentials;

    public LoginDialog()
    {
      setTitle("Login");
      setModal(true);

      final JTextField userField = new JTextField(15);
      final JPasswordField passwordField = new JPasswordField(15);

      Container contentPane = getContentPane();
      contentPane.setLayout(new GridLayout(3, 2));
      contentPane.add(new JLabel("Username:"));
      contentPane.add(userField);
      contentPane.add(new JLabel("Password:"));
      contentPane.add(passwordField);

      JButton okButton = new JButton("OK");
      okButton.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          String username = userField.getText();
          String password = new String(passwordField.getPassword());
          if (!isEmpty(username) && !isEmpty(password))
          {
            credentials = new Credentials(username, password);
          }

          close();
        }
      });

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          close();
        }
      });

      contentPane.add(okButton);
      contentPane.add(cancelButton);

      userField.setText("eclipse_test_123456789");
      passwordField.requestFocus();

      pack();
      setCenterLocation();
      setVisible(true);
    }

    public Credentials getCredentials()
    {
      return credentials;
    }

    private void setCenterLocation()
    {
      try
      {
        Point mousePoint = MouseInfo.getPointerInfo().getLocation();
        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
        {
          Rectangle bounds = device.getDefaultConfiguration().getBounds();
          if (bounds.contains(mousePoint))
          {
            setLocation((bounds.width - getWidth()) / 2 + bounds.x, (bounds.height - getHeight()) / 2 + bounds.y);
            break;
          }
        }
      }
      catch (Exception ex)
      {
        //$FALL-THROUGH$
      }
    }

    private void close()
    {
      setVisible(false);
      dispose();
    }

    private static boolean isEmpty(String str)
    {
      return str == null || str.length() == 0;
    }
  }
}
