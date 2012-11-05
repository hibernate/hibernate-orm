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
import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardOperation;
import org.hibernate.shards.strategy.exit.ExitOperationsCollector;
import org.hibernate.shards.strategy.exit.ExitStrategy;

import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class SequentialShardAccessStrategy implements ShardAccessStrategy {

  private final Log log = LogFactory.getLog(getClass());

  public <T> T apply(List<Shard> shards, ShardOperation<T> operation, ExitStrategy<T> exitStrategy, ExitOperationsCollector exitOperationsCollector) {
    for(Shard shard : getNextOrderingOfShards(shards)) {
      if(exitStrategy.addResult(operation.execute(shard), shard)) {
        log.debug(
            String.format(
                "Short-circuiting operation %s after execution against shard %s",
                operation.getOperationName(),
                shard));
        break;
      }
    }
    return exitStrategy.compileResults(exitOperationsCollector);
  }

  /**
   * Override this method if you want to control the order in which the
   * shards are operated on (this comes in handy when paired with exit
   * strategies that allow early exit because it allows you to evenly
   * distribute load).  Deafult implementation is to just iterate in the
   * same order every time.
   * @param shards The shards we might want to reorder
   * @return Reordered view of the shards.
   */
  protected Iterable<Shard> getNextOrderingOfShards(List<Shard> shards) {
    return shards;
  }
}
