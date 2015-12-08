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

import org.eclipse.core.runtime.IProgressMonitor;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * @author Eike Stepper
 */
public abstract class AbstractTest extends CoreMatchers
{
  private static final String[] FILTERS = { //
      "org.eclipse.jdt.internal.junit.runner.", //
      "org.eclipse.jdt.internal.junit.ui.", //
      "org.eclipse.jdt.internal.junit4.runner.", //
      "org.junit.", //
      "sun.reflect.", //
      "java.lang.reflect.Method.invoke(", //
      "junit.framework.Assert", //
      "junit.framework.TestCase", //
      "junit.framework.TestResult", //
      "junit.framework.TestResult$1", //
      "junit.framework.TestSuite", //
  };

  private static final PrintStream LOG = System.out;

  public static final IProgressMonitor LOGGER = new IProgressMonitor()
  {
    private String message = "";

    @Override
    public void beginTask(String name, int totalWork)
    {
      filter(name);
    }

    @Override
    public void done()
    {
    }

    @Override
    public void internalWorked(double work)
    {
    }

    @Override
    public boolean isCanceled()
    {
      return false;
    }

    @Override
    public void setCanceled(boolean value)
    {
    }

    @Override
    public void setTaskName(String name)
    {
      filter(name);
    }

    @Override
    public void subTask(String name)
    {
      filter(name);
    }

    @Override
    public void worked(int work)
    {
    }

    private void filter(String message)
    {
      if (message == null)
      {
        message = "";
      }

      if (!message.equals(this.message))
      {
        log(message);
      }

      this.message = message;
    }
  };

  @Rule
  public final TestWatcher failurePrinter = new FailurePrinter();

  @Rule
  public final TestName testName = new TestName();

  @Before
  public void setUp() throws Exception
  {
    log("=========================================================================================================================\n" //
        + testName.getMethodName() + "\n" //
        + "=========================================================================================================================");
  }

  @After
  public void tearDown() throws Exception
  {
    log();
    LOGGER.setTaskName(null);
  }

  public static void log()
  {
    LOG.println();
  }

  public static void log(Object object)
  {
    if (object instanceof Throwable)
    {
      Throwable ex = (Throwable)object;
      printStackTrace(ex);
    }
    else
    {
      LOG.println(object);
    }
  }

  public static void printStackTrace(Throwable ex)
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ex.printStackTrace(new PrintStream(baos));

    BufferedReader reader = new BufferedReader(new InputStreamReader(IOUtil.streamUTF(baos.toByteArray())));
    String line;
    StringBuilder builder = new StringBuilder();

    try
    {
      LOOP: while ((line = reader.readLine()) != null)
      {
        for (int i = 0; i < FILTERS.length; i++)
        {
          String filter = FILTERS[i];
          if (line.trim().startsWith("at " + filter))
          {
            continue LOOP;
          }
        }

        builder.append(line);
        builder.append('\n');
      }
    }
    catch (IOException ignore)
    {
      // Should not happen.
      //$FALL-THROUGH$
    }

    System.err.println(builder);
  }

  public static void fail(String message) throws AssertionError
  {
    throw new AssertionError(message);
  }

  public static org.hamcrest.Matcher<java.lang.Object> isNull()
  {
    return org.hamcrest.core.IsNull.nullValue();
  }

  public static org.hamcrest.Matcher<java.lang.Object> isNotNull()
  {
    return org.hamcrest.core.IsNull.notNullValue();
  }

  /**
   * @author Eike Stepper
   */
  private static final class FailurePrinter extends TestWatcher
  {
    @Override
    protected void failed(Throwable ex, Description description)
    {
      printStackTrace(ex);
    }
  }
}
