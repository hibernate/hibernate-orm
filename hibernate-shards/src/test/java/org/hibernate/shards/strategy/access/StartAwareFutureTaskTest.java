/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.strategy.access;

import junit.framework.TestCase;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * @author maxr@google.com (Max Ross)
 */
public class StartAwareFutureTaskTest extends TestCase {

  /**
   * This test demonstrates that cancelling an in progress
   * FutureTask returns true.
   */
  public void testCancelBehavior() {
    Runnable r = new Runnable() {
      public synchronized void run() {
        try {
          wait();
        } catch (InterruptedException e) {
          fail("unexpected interrupted exception");
        }
      }
    };

    FutureTask<Object> ft = new FutureTask<Object>(r, null);
    Thread t = new Thread(ft);
    try {
      t.start();
      assertThreadStateEquals(Thread.State.WAITING, t);
      assertTrue(ft.cancel(false));
      assertTrue(ft.isCancelled());
    } finally {
      synchronized(t) {
        t.notify();
      }
    }
  }

  /**
   * This test demonstrates that cancelling an in progress
   * StartAwareFutureTask returns false.
   */
  public void testCustomCancelBehavior() {
    Callable<Void> c = new Callable<Void>() {
      public synchronized Void call() throws Exception {
        try {
          wait();
        } catch (InterruptedException e) {
          fail("unexpected interrupted exception");
        }
        return null;
      }
    };

    StartAwareFutureTask ft = new StartAwareFutureTask(c, 0);
    Thread t = new Thread(ft);
    try {
      t.start();
      assertThreadStateEquals(Thread.State.WAITING, t);
      assertFalse(ft.cancel(false));
      assertFalse(ft.isCancelled());
    } finally {
      synchronized(t) {
        t.notify();
      }
    }
  }

  public void testCustomCancelBehaviorWhenRunning() {
    Callable<Void> c = new Callable<Void>() {
      public Void call() {
        throw new UnsupportedOperationException();
      }
    };

    StartAwareFutureTask ft = new StartAwareFutureTask(c, 0);
    assertFalse(ft.cancelled);
    assertFalse(ft.runCalled);
    assertTrue(ft.cancel(false));
    assertTrue(ft.cancelled);
    assertFalse(ft.runCalled);
    ft.run();
    assertFalse(ft.runCalled);
  }

  public void testCustomCancelBehaviorWhenCancelling() {
    Callable<Void> c = new Callable<Void>() {
      public Void call() {
        return null;
      }
    };

    StartAwareFutureTask ft = new StartAwareFutureTask(c, 0) {
      @Override
      boolean superCancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
      }
    };
    assertFalse(ft.cancelled);
    assertFalse(ft.runCalled);
    ft.run();
    assertFalse(ft.cancelled);
    assertTrue(ft.runCalled);
    ft.cancel(false);
    assertFalse(ft.cancelled);
  }

  private void assertThreadStateEquals(Thread.State expectedState, Thread t) {
    long startTime = System.currentTimeMillis();
    long maxElapsed = 500;
    while(t.getState() != expectedState) {
      if(System.currentTimeMillis() - startTime >= maxElapsed) {
        fail("Thread never arrived in expected state " + expectedState);
      }
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {

      }
    }
  }
}
