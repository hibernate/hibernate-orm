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

package org.hibernate.shards.integration;

/**
 * @author maxr@google.com (Max Ross)
 */
final class Permutation {

  static final Permutation DEFAULT = new Permutation(IdGenType.SIMPLE, ShardAccessStrategyType.SEQUENTIAL, 3, 3, false);

  private final IdGenType idGenType;
  private final ShardAccessStrategyType sast;
  private final int numDbs;
  private final int numShards;
  private final boolean virtualShardingEnabled;

  public Permutation(IdGenType idGenType, ShardAccessStrategyType sast,
      int numDbs) {
    this(idGenType, sast, numDbs, numDbs, false);
  }

  public Permutation(IdGenType idGenType, ShardAccessStrategyType sast,
      int numDbs, int numShards, boolean virtualShardingEnabled) {
    this.idGenType = idGenType;
    this.sast = sast;
    this.numDbs = numDbs;
    this.numShards = numShards;
    this.virtualShardingEnabled = virtualShardingEnabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Permutation that = (Permutation) o;

    if (numDbs != that.numDbs) {
      return false;
    }
    if (numShards != that.numShards) {
      return false;
    }
    if (idGenType != that.idGenType) {
      return false;
    }
    if (virtualShardingEnabled != that.virtualShardingEnabled) {
      return false;
    }
    return sast == that.sast;
  }

  @Override
  public int hashCode() {
    int result;
    result = idGenType.hashCode();
    result = 29 * result + sast.hashCode();
    result = 29 * result + numDbs;
    result = 29 * result + numShards;
    result = 29 * result + (virtualShardingEnabled ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return idGenType.name() + " - " + sast.name() + " - " + numDbs + (virtualShardingEnabled ? " - " + numShards + " VIRTUAL SHARDS" : "");
  }

  public IdGenType getIdGenType() {
    return idGenType;
  }

  public ShardAccessStrategyType getSast() {
    return sast;
  }

  public int getNumDbs() {
    return numDbs;
  }

  public int getNumShards() {
    return numShards;
  }

  public boolean isVirtualShardingEnabled() {
    return virtualShardingEnabled;
  }

  public String getMessageWithPermutationPrefix(String msg) {
    return toString() + ": " + msg;
  }
}
