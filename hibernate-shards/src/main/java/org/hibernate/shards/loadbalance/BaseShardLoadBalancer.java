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

import org.hibernate.shards.BaseHasShardIdList;
import org.hibernate.shards.ShardId;

import java.util.List;

/**
 * Helpful base class for ShardLoadBalancer implementations.
 *
 * @author maxr@google.com (Max Ross)
 */
public abstract class BaseShardLoadBalancer extends BaseHasShardIdList implements ShardLoadBalancer {

  /**
   * Construct a BaseShardLoadBalancer
   * @param shardIds the ShardIds that we're going to balance across
   */
  protected BaseShardLoadBalancer(List<ShardId> shardIds) {
    super(shardIds);
  }

  public ShardId getNextShardId() {
    return shardIds.get(getNextIndex());
  }

  /**
   * @return the index of the next ShardId we should return
   */
  protected abstract int getNextIndex();
}
