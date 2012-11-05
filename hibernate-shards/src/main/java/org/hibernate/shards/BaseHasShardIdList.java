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

package org.hibernate.shards;

import org.hibernate.shards.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation for HasShadIdList.
 * Takes care of null/empty checks.
 *
 * @author maxr@google.com (Max Ross)
 */
public abstract class BaseHasShardIdList implements HasShardIdList {

  // our list of {@link ShardId} objects
  protected final List<ShardId> shardIds;

  /**
   * Construct a BaseHasShardIdList.  {@link List} cannot be empty
   * @param shardIds  the {@link ShardId}s
   */
  protected BaseHasShardIdList(List<ShardId> shardIds) {
    Preconditions.checkNotNull(shardIds);
    Preconditions.checkArgument(!shardIds.isEmpty());
    // make our own copy to be safe
    this.shardIds = new ArrayList<ShardId>(shardIds);
  }

  public List<ShardId> getShardIds() {
    return Collections.unmodifiableList(shardIds);
  }

}
