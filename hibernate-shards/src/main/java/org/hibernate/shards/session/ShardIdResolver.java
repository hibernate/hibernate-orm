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

package org.hibernate.shards.session;

import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardId;

import java.util.List;

/**
 * Interface for objects that are able to resolve shard of objects.
 *
 * @author maxr@google.com (Max Ross)
 */
interface ShardIdResolver {

  /**
   * Gets ShardId of the shard given object lives on. Only consideres given
   * Shards.
   *
   * @param obj Object whose Shard should be resolved
   * @param shardsToConsider Shards which should be considered during resolution
   * @return ShardId of the shard the object lives on; null if shard could not be resolved
   */
  /*@Nullable*/ ShardId getShardIdForObject(Object obj, List<Shard> shardsToConsider);

  /**
   * Gets ShardId of the shard given object lives on.
   *
   * @param obj Object whose Shard should be resolved
   * @return ShardId of the shard the object lives on; null if shard could not be resolved
   */
  /*@Nullable*/ ShardId getShardIdForObject(Object obj);
}
