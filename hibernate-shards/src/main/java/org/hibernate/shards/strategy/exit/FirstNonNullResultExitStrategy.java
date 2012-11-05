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

package org.hibernate.shards.strategy.exit;

import org.hibernate.shards.Shard;
import org.hibernate.shards.util.Preconditions;

/**
 * Threadsafe ExitStrategy implementation that only accepts the first result
 * added.
 *
 * @author maxr@google.com (Max Ross)
 */
public class FirstNonNullResultExitStrategy<T> implements ExitStrategy<T> {

  private T nonNullResult;
  private Shard shard;

  /**
   * Synchronized method guarantees that only the first thread to add a result
   * will have its result reflected.
   */
  public final synchronized boolean addResult(T result, Shard shard) {
    Preconditions.checkNotNull(shard);
    if(result != null && nonNullResult == null) {
      nonNullResult = result;
      this.shard = shard;
      return true;
    }
    return false;
  }

  public T compileResults(ExitOperationsCollector exitOperationsCollector) {
    return nonNullResult;
  }

  public Shard getShardOfResult() {
    return shard;
  }
}
