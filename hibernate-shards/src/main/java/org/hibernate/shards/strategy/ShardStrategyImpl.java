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

package org.hibernate.shards.strategy;

import org.hibernate.shards.strategy.access.ShardAccessStrategy;
import org.hibernate.shards.strategy.resolution.ShardResolutionStrategy;
import org.hibernate.shards.strategy.selection.ShardSelectionStrategy;
import org.hibernate.shards.util.Preconditions;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ShardStrategyImpl implements ShardStrategy {

  private final ShardSelectionStrategy shardSelectionStrategy;
  private final ShardResolutionStrategy shardResolutionStrategy;
  private final ShardAccessStrategy shardAccessStrategy;

  public ShardStrategyImpl(
      ShardSelectionStrategy shardSelectionStrategy,
      ShardResolutionStrategy shardResolutionStrategy,
      ShardAccessStrategy shardAccessStrategy) {
    Preconditions.checkNotNull(shardSelectionStrategy);
    Preconditions.checkNotNull(shardResolutionStrategy);
    Preconditions.checkNotNull(shardAccessStrategy);
    this.shardSelectionStrategy = shardSelectionStrategy;
    this.shardResolutionStrategy = shardResolutionStrategy;
    this.shardAccessStrategy = shardAccessStrategy;
  }

  public ShardSelectionStrategy getShardSelectionStrategy() {
    return shardSelectionStrategy;
  }

  public ShardResolutionStrategy getShardResolutionStrategy() {
    return shardResolutionStrategy;
  }

  public ShardAccessStrategy getShardAccessStrategy() {
    return shardAccessStrategy;
  }
}
