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
package org.eclipse.userstorage.ui;

import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.ui.internal.Activator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.lang.reflect.Field;

/**
 * @author Eike Stepper
 */
public abstract class AbstractDialog extends TitleAreaDialog
{
  private static final String DIALOG_WIDTH = getDialogConstant("DIALOG_WIDTH", "DIALOG_WIDTH");

  private static final String DIALOG_HEIGHT = getDialogConstant("DIALOG_HEIGHT", "DIALOG_HEIGHT");

  private static final String DIALOG_FONT_DATA = getDialogConstant("DIALOG_FONT_DATA", "DIALOG_FONT_NAME");

  private static final String DIALOG_MESSAGE = "DIALOG_MESSAGE";

  private static final int WIDTH_MIN = 350;

  private static final int WIDTH_MAX = 5000;

  private static final int WIDTH_INC1 = 250;

  private static final int WIDTH_INC2 = WIDTH_INC1 / 10;

  private static final Field messageLabelField;

  static
  {
    Field field = null;

    try
    {
      field = TitleAreaDialog.class.getDeclaredField("messageLabel");
      field.setAccessible(true);
    }
    catch (Throwable ex)
    {
      field = null;
    }

    messageLabelField = field;
  }

  public AbstractDialog(Shell parentShell)
  {
    super(parentShell);
    setShellStyle(SWT.SHELL_TRIM | SWT.BORDER | SWT.APPLICATION_MODAL);
  }

  @Override
  public boolean close()
  {
    Shell shell = getShell();
    if (shell != null && !shell.isDisposed())
    {
      IDialogSettings settings = getDialogBoundsSettings();
      if (settings != null)
      {
        String message = getMessage();

        int strategy = getDialogBoundsStrategy();
        if ((strategy & DIALOG_PERSISTSIZE) != 0)
        {
          settings.put(DIALOG_MESSAGE, message);
        }
      }
    }

    return super.close();
  }

  protected IDialogSettings getPluginSettings()
  {
    return null;
  }

  protected String getDialogSettingsName()
  {
    return getClass().getSimpleName();
  }

  @Override
  protected IDialogSettings getDialogBoundsSettings()
  {
    IDialogSettings settings = getPluginSettings();
    if (settings == null)
    {
      return null;
    }

    String sectionName = getDialogSettingsName();
    if (sectionName == null)
    {
      return null;
    }

    IDialogSettings section = settings.getSection(sectionName);
    if (section == null)
    {
      section = settings.addNewSection(sectionName);
    }

    return section;
  }

  @Override
  protected int getDialogBoundsStrategy()
  {
    return DIALOG_PERSISTSIZE;
  }

  @Override
  protected Point getInitialSize()
  {
    Shell shell = getShell();
    Point result = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

    Point minimum = getMinimumSize();
    result.x = Math.max(result.x, minimum.x);
    result.y = Math.max(result.y, minimum.y);

    if ((getDialogBoundsStrategy() & DIALOG_PERSISTSIZE) != 0)
    {
      IDialogSettings settings = getDialogBoundsSettings();
      if (settings != null)
      {
        if (hasSameFont(settings) && hasSameMessage(settings))
        {
          try
          {
            int width = settings.getInt(DIALOG_WIDTH);
            if (width != DIALOG_DEFAULT_BOUNDS)
            {
              result.x = width;
            }
          }
          catch (NumberFormatException ex)
          {
            //$FALL-THROUGH$
          }

          try
          {
            int height = settings.getInt(DIALOG_HEIGHT);
            if (height != DIALOG_DEFAULT_BOUNDS)
            {
              result.y = height;
            }
          }
          catch (NumberFormatException ex)
          {
            //$FALL-THROUGH$
          }

          return result;
        }
      }
    }

    Text messageLabel;

    try
    {
      messageLabel = (Text)messageLabelField.get(this);
    }
    catch (Throwable ex)
    {
      return result;
    }

    String message = messageLabel.getText();
    messageLabel.setText("\n\n");

    int messageHeight = messageLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
    messageLabel.setText(message);

    result.x = WIDTH_MIN;
    while (result.x < WIDTH_MAX)
    {
      result.x += WIDTH_INC1;
      shell.setSize(result);

      Point messageSize = messageLabel.computeSize(messageLabel.getSize().x, SWT.DEFAULT);
      if (messageSize.y <= messageHeight)
      {
        break;
      }
    }

    result.x -= WIDTH_INC1;
    while (result.x < WIDTH_MAX)
    {
      result.x += WIDTH_INC2;
      shell.setSize(result);

      Point messageSize = messageLabel.computeSize(messageLabel.getSize().x, SWT.DEFAULT);
      if (messageSize.y <= messageHeight)
      {
        break;
      }
    }

    result = shell.computeSize(result.x + WIDTH_INC2, SWT.DEFAULT, true);
    return result;
  }

  protected Point getMinimumSize()
  {
    return new Point(WIDTH_MIN, 0);
  }

  @Override
  protected Control createDialogArea(Composite parent)
  {
    Shell shell = getShell();

    ImageDescriptor descriptor = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/LoginBanner.png");
    final Image titleImage = descriptor.createImage(shell.getDisplay());

    shell.addDisposeListener(new DisposeListener()
    {
      @Override
      public void widgetDisposed(DisposeEvent e)
      {
        titleImage.dispose();
      }
    });

    setTitleImage(titleImage);
    return super.createDialogArea(parent);
  }

  private boolean hasSameFont(IDialogSettings settings)
  {
    String previousFontData = settings.get(DIALOG_FONT_DATA);
    if (StringUtil.isEmpty(previousFontData))
    {
      return false;
    }

    FontData[] fontDatas = JFaceResources.getDialogFont().getFontData();
    if (fontDatas.length == 0)
    {
      return false;
    }

    String currentFontData = fontDatas[0].toString();
    return previousFontData.equalsIgnoreCase(currentFontData);
  }

  private boolean hasSameMessage(IDialogSettings settings)
  {
    String previousMessage = settings.get(DIALOG_MESSAGE);
    if (StringUtil.isEmpty(previousMessage))
    {
      return false;
    }

    String currentMessage = getMessage();
    return previousMessage.equals(currentMessage);
  }

  private static String getDialogConstant(String name, String defaultValue)
  {
    try
    {
      Field field = Dialog.class.getDeclaredField(name);
      field.setAccessible(true);

      return (String)field.get(null);
    }
    catch (Throwable ex)
    {
      //$FALL-THROUGH$
    }

    return defaultValue;
  }
}
