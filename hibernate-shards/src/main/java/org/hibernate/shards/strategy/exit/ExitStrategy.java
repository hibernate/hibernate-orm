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

/**
 * Classes implementing this interface gather results from operations that are
 * executed across shards.  If you intend to use a specific implementation
 * in conjunction with ParallelShardAccessStrategy that implementation must
 * be threadsafe.
 *
 * @author maxr@google.com (Max Ross)
 */
public interface ExitStrategy<T> {

  /**
   * Add the provided result and return whether or not the caller can halt
   * processing.
   * @param result The result to add
   * @return Whether or not the caller can halt processing
   */
  boolean addResult(T result, Shard shard);

  T compileResults(ExitOperationsCollector exitOperationsCollector);
}
