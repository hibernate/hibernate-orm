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

import org.hibernate.shards.Shard;
import org.hibernate.shards.util.Iterables;

import java.util.List;
import java.util.Random;

/**
 * A SequentialShardAccessStrategy starts with the first Shard in the list
 * every time.  If the ExitStrategy with which the AccessStrategy is paired
 * supports early exit (keep searching until you have 100 results), the first
 * shard in the list may receive a disproportionately high percentage of the
 * queries.  In order to combat this we have a load balanced approach that
 * adjusts that provides a rotated view of the list of shards.  The list is
 * rotated by a different amount each time.  The amount by which we rotate
 * is random because doing a true round-robin would require that we know
 * the shards we're rotating in advance, but the shards passed to a
 * ShardAccessStrategy can vary between invocations.
 *
 * @author maxr@google.com (Max Ross)
 */
public class LoadBalancedSequentialShardAccessStrategy extends SequentialShardAccessStrategy {

  private final Random rand;

  public LoadBalancedSequentialShardAccessStrategy() {
    this.rand = new Random(System.currentTimeMillis());
  }

  @Override
  protected Iterable<Shard> getNextOrderingOfShards(List<Shard> shards) {
    return Iterables.rotate(shards, rand.nextInt() % shards.size());
  }

}
