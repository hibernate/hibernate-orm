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

package org.hibernate.shards.loadbalance;

import org.hibernate.shards.ShardId;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round robin load balancing algorithm.
 *
 * @author maxr@google.com (Max Ross)
 */
public class RoundRobinShardLoadBalancer extends BaseShardLoadBalancer {

  // Can be shared by multiple threads so access to the counter
  // needs to be threadsafe.
  private final AtomicInteger nextIndex = new AtomicInteger();

  /**
   * Construct a RoundRobinShardLoadBalancer
   * @param shardIds the ShardIds that we're balancing across
   */
  public RoundRobinShardLoadBalancer(List<ShardId> shardIds) {
    super(shardIds);
  }

  @Override
  protected int getNextIndex() {
    return nextIndex.getAndIncrement() % getShardIds().size();
  }
}

