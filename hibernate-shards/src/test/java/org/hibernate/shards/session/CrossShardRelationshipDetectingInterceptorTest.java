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

import junit.framework.TestCase;
import org.hibernate.shards.CrossShardAssociationException;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.defaultmock.TypeDefaultMock;
import org.hibernate.shards.util.Pair;
import org.hibernate.type.Type;

import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class CrossShardRelationshipDetectingInterceptorTest extends TestCase {

  public void testCheckForConflictingShardId() {
    ShardId shardId = new ShardId(0);
    final ShardId[] shardIdToReturn = {null};
    ShardIdResolver resolver = new ShardIdResolverDefaultMock() {
      @Override
      public ShardId getShardIdForObject(Object obj) {
        return shardIdToReturn[0];
      }
    };
    CrossShardRelationshipDetectingInterceptor crdi = new CrossShardRelationshipDetectingInterceptor(resolver);
    Object obj = new Object();
    crdi.checkForConflictingShardId("yam", shardId, obj);
    shardIdToReturn[0] = shardId;
    crdi.checkForConflictingShardId("yam", shardId, obj);
    shardIdToReturn[0] = new ShardId(1);
    try {
      crdi.checkForConflictingShardId("yam", shardId, obj);
      fail("Expected Cross Shard Association Exception");
    } catch (CrossShardAssociationException csae) {
      // good
    }
  }

  private static final class MyType extends TypeDefaultMock {

    private final boolean isAssociationType;

    public MyType(boolean associationType) {
      isAssociationType = associationType;
    }

    @Override
    public boolean isAssociationType() {
      return isAssociationType;
    }
  }

  public void testAssociationPairFilter() {
    Pair<Type, Object> expectedPair = Pair.<Type, Object>of(new MyType(true), "yam");
    Type[] types = {new MyType(false), new MyType(true), new MyType(false), expectedPair.first};
    Object[] objects = {null, null, "yam", expectedPair.second};
    List<Pair<Type, Object>> list = CrossShardRelationshipDetectingInterceptor.buildListOfAssociations(types, objects);
    assertEquals(1, list.size());
    Pair<Type, Object> pair = list.get(0);
    assertNotNull(pair);
    assertEquals(expectedPair, pair);
  }
}
