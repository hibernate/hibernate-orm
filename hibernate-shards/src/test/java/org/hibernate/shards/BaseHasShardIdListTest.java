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

import junit.framework.TestCase;
import org.hibernate.shards.util.Lists;

import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class BaseHasShardIdListTest extends TestCase {

  public void testShardIdListUnmodifiable() {
    List<ShardId> shardIdList = null;
    try {
      new MyBaseHasShardIdList(shardIdList);
      fail("expected npe");
    } catch (NullPointerException npe) {
      // good
    }
    shardIdList = Lists.newArrayList();
    try {
      new MyBaseHasShardIdList(shardIdList);
      fail("expected iae");
    } catch (IllegalArgumentException iae) {
      // good
    }
    shardIdList.add(new ShardId(0));
    BaseHasShardIdList bhsil = new MyBaseHasShardIdList(shardIdList);
    ShardId anotherId = new ShardId(1);
    shardIdList.add(anotherId);
    // demonstrate that external changes to the list that was passed in
    // aren't reflected inside the object
    assertFalse(bhsil.shardIds.contains(anotherId));
  }

  private static final class MyBaseHasShardIdList extends BaseHasShardIdList {

    protected MyBaseHasShardIdList(List<ShardId> shardIds) {
      super(shardIds);
    }

    public ShardId selectShardIdForNewObject(Object obj) {
      throw new UnsupportedOperationException();
    }
  }
}
