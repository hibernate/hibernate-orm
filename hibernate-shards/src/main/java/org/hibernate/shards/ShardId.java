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

/**
 * Uniquely identifies a virtual shard.
 *
 * @author maxr@google.com (Max Ross)
 */
public class ShardId {

  private final int shardId;

  public ShardId(int shardId) {
    this.shardId = shardId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ShardId shardId1 = (ShardId)o;

    return shardId == shardId1.shardId;
  }

  @Override
  public int hashCode() {
    return shardId;
  }

  public int getId() {
    return shardId;
  }

  @Override
  public String toString() {
    return Integer.toString(shardId);
  }
}
