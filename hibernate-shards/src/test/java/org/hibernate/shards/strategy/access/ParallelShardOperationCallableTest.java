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
import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardDefaultMock;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.ShardOperation;
import org.hibernate.shards.ShardOperationDefaultMock;
import org.hibernate.shards.strategy.exit.ExitStrategy;
import org.hibernate.shards.strategy.exit.ExitStrategyDefaultMock;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Sets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ParallelShardOperationCallableTest extends TestCase {

  public void testCountDown() throws Exception {
    int latchSize = 5;
    CountDownLatch startLatch = new CountDownLatch(0);
    CountDownLatch latch = new CountDownLatch(latchSize);
    final boolean[] addResultResult = {false};
    ExitStrategy<Void> strat = new ExitStrategyDefaultMock<Void>() {
      @Override      
      public boolean addResult(Void result, Shard shard) {
        return addResultResult[0];
      }
    };
    ShardOperation<Void> operation = new ShardOperationDefaultMock<Void>() {
      @Override
      public Void execute(Shard shard) {
        return null;
      }

      @Override
      public String getOperationName() {
        return "yam";
      }
    };
    Shard shard = new ShardDefaultMock() {
      @Override
      public String toString() {
        return "yam";
      }

      @Override
      public Set<ShardId> getShardIds() {
        return Sets.newHashSet(new ShardId(0));
      }
    };
    List<StartAwareFutureTask> futureTasks = Lists.newArrayList();
    ParallelShardOperationCallable<Void> callable = new ParallelShardOperationCallable<Void>(
        startLatch,
        latch,
        strat,
        operation,
        shard,
        futureTasks);

    callable.call();
    // addResult returns false so latch is only decremented by 1
    assertEquals(latchSize - 1, latch.getCount());
    // addResult returns false so latch is only decremented by 1
    callable.call();
    assertEquals(latchSize - 2, latch.getCount());

    // now addResult returns true
    addResultResult[0] = true;
    Callable<Void> anotherCallable = new Callable<Void>() {
      public Void call() {
        return null;
      }
    };
    StartAwareFutureTask ft = new StartAwareFutureTask(anotherCallable, 0) {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
      }
    };
    futureTasks.add(ft);
    callable.call();
    // cancelling the 1 task returns false, so latch is only decremented by 1
    assertEquals(latchSize - 3, latch.getCount());

    // add a second task that returns true when cancelled
    ft = new StartAwareFutureTask(anotherCallable, 0) {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return true;
      }
    };
    futureTasks.add(ft);
    callable.call();
    // 1 decrement for myself and 1 for the task that returned true when cancelled
    assertEquals(latchSize - 5, latch.getCount());
  }

}
