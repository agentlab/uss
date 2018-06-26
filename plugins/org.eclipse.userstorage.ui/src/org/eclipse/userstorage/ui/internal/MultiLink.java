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
package org.eclipse.userstorage.ui.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Eike Stepper
 */
public abstract class MultiLink extends Composite
{
  private static final Pattern PATTERN = Pattern.compile("<a href=[\"']([^\"']+)[\"']>([^<]+)</a>");

  private final Map<String, String> hrefs = new HashMap<String, String>();

  private Link link;

  private String text;

  public MultiLink(Composite parent, int style)
  {
    super(parent, SWT.NONE);
    setLayout(new FillLayout());

    link = new Link(this, style);
    link.addSelectionListener(new SelectionAdapter()
    {
      @Override
      public void widgetSelected(SelectionEvent e)
      {
        String label = e.text;

        String href = hrefs.get(label);
        if (href != null)
        {
          linkSelected(label, href);
        }
      }
    });
  }

  public String getText()
  {
    return text;
  }

  public void setText(String text)
  {
    this.text = text;

    hrefs.clear();
    StringBuffer buffer = new StringBuffer();

    Matcher matcher = PATTERN.matcher(text);
    while (matcher.find())
    {
      String href = matcher.group(1);
      String label = matcher.group(2);
      hrefs.put(label, href);
      matcher.appendReplacement(buffer, "<a>" + label + "</a>");
    }

    matcher.appendTail(buffer);
    link.setText(buffer.toString());
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    super.setEnabled(enabled);
    link.setEnabled(enabled);
  }

  @Override
  public void setVisible(boolean visible)
  {
    super.setVisible(visible);
    link.setVisible(visible);
  }

  @Override
  public boolean setFocus()
  {
    return link.setFocus();
  }

  protected abstract void linkSelected(String label, String href);

  /**
   * @author Eike Stepper
   */
  public static class ForSystemBrowser extends MultiLink
  {
    public ForSystemBrowser(Composite parent, int style)
    {
      super(parent, style);
    }

    @Override
    protected void linkSelected(String label, String href)
    {
      SystemBrowser.openSafe(getShell(), href, "Go to " + href + " to read the " + label + ".");
    }
  }
}
