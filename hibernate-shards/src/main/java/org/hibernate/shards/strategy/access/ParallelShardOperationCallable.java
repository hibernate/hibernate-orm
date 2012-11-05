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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardOperation;
import org.hibernate.shards.strategy.exit.ExitStrategy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Runs a single operation on a single shard, collecting the result of the
 * operation with an ExitStrategy.  The interesting bit here is that
 * if the ExitStrategy indicates that there is no more work to be performed,
 * this object has the ability to cancel the work being performed by all the
 * other threads.
 *
 * @author maxr@google.com (Max Ross)
 */
class ParallelShardOperationCallable<T> implements Callable<Void> {

  private static final boolean INTERRUPT_IF_RUNNING = false;

  private final Log log = LogFactory.getLog(getClass());

  private final CountDownLatch startSignal;

  private final CountDownLatch doneSignal;

  private final ExitStrategy<T> exitStrategy;

  private final ShardOperation<T> operation;

  private final Shard shard;

  private final List<StartAwareFutureTask> futureTasks;

  public ParallelShardOperationCallable(
      CountDownLatch startSignal,
      CountDownLatch doneSignal,
      ExitStrategy<T> exitStrategy,
      ShardOperation<T> operation,
      Shard shard,
      List<StartAwareFutureTask> futureTasks) {
    this.startSignal = startSignal;
    this.doneSignal = doneSignal;
    this.exitStrategy = exitStrategy;
    this.operation = operation;
    this.shard = shard;
    this.futureTasks = futureTasks;
  }

  public Void call() throws Exception {
    try {
      waitForStartSignal();
      log.debug(String.format("Starting execution of %s against shard %s",  operation.getOperationName(), shard));
      /**
       * If addResult() returns true it means there is no more work to be
       * performed.  Cancel all the outstanding tasks.
       */
      if(exitStrategy.addResult(operation.execute(shard), shard)) {
        log.debug(
            String.format(
                "Short-circuiting execution of %s on other threads after execution against shard %s",
                operation.getOperationName(),
                shard));
        /**
         * It's ok to cancel ourselves because StartAwareFutureTask.cancel()
         * will return false if a task has already started executing, and we're
         * already executing.
         */

        log.debug(String.format("Checking %d future tasks to see if they need to be cancelled.", futureTasks.size()));
        for(StartAwareFutureTask ft : futureTasks) {
          log.debug(String.format("Preparing to cancel future task %d.", ft.getId()));
          /**
           * If a task was successfully cancelled that means it had not yet
           * started running.  Since the task won't run, the task won't be
           * able to decrement the CountDownLatch.  We need to decrement
           * it on behalf of the cancelled task.
           */
          if(ft.cancel(INTERRUPT_IF_RUNNING)) {
            log.debug("Task cancel returned true, decrementing counter on its behalf.");
            doneSignal.countDown();
          } else {
            log.debug("Task cancel returned false, not decrementing counter on its behalf.");
          }
        }
      } else {
        log.debug(
            String.format(
                "No need to short-cirtcuit execution of %s on other threads after execution against shard %s",
                operation.getOperationName(),
                shard));
      }
    } finally {
      // counter must get decremented no matter what
      log.debug(String.format("Decrementing counter for operation %s on shard %s", operation.getOperationName(), shard));
      doneSignal.countDown();
    }
    return null;
  }

  private void waitForStartSignal() {
    try {
      startSignal.await();
    } catch (InterruptedException e) {
      // I see no reason why this should happen
      final String msg = String.format("Received interrupt while waiting to begin execution of %s against shard %s", operation.getOperationName(), shard);
      log.error(msg);
      throw new HibernateException(msg);
    }
  }
}

