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

package org.hibernate.shards.criteria;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.defaultmock.CriteriaDefaultMock;
import org.hibernate.shards.defaultmock.CriteriaEventDefaultMock;
import org.hibernate.shards.defaultmock.ShardDefaultMock;
import org.hibernate.shards.defaultmock.ShardedCriteriaDefaultMock;
import org.hibernate.shards.defaultmock.SubcriteriaFactoryDefaultMock;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ShardedSubcriteriaImplTest {

    @Test
    public void testInit() {
        final int[] next = {0};
        class MyShard extends ShardDefaultMock {
            @Override
            public Set<ShardId> getShardIds() {
                return Sets.newHashSet(new ShardId(next[0]++));
            }
        }
        Shard s1 = new MyShard();
        Shard s2 = new MyShard();
        Shard s3 = new MyShard();
        List<Shard> shards = Lists.newArrayList(s1, s2, s3);
        ShardedSubcriteriaImpl ss = new ShardedSubcriteriaImpl(shards, new ShardedCriteriaDefaultMock());
        Assert.assertEquals(shards.size(), ss.getShardToCriteriaMap().size());
        Assert.assertEquals(shards.size(), ss.getShardToEventListMap().size());
    }

    @Test
    public void testListDelegatesToParent() {
        List<Shard> shards = Lists.<Shard>newArrayList(new ShardDefaultMock());
        final boolean[] called = {false};
        ShardedCriteria parent = new ShardedCriteriaDefaultMock() {
            @Override
            public List list() throws HibernateException {
                called[0] = true;
                return null;
            }
        };
        ShardedSubcriteriaImpl ss = new ShardedSubcriteriaImpl(shards, parent);
        ss.list();
        Assert.assertTrue(called[0]);
    }

    @Test
    public void testUniqueResultDelegatesToParent() {
        List<Shard> shards = Lists.<Shard>newArrayList(new ShardDefaultMock());
        final boolean[] called = {false};
        ShardedCriteria parent = new ShardedCriteriaDefaultMock() {
            @Override
            public Object uniqueResult() throws HibernateException {
                called[0] = true;
                return null;
            }
        };
        ShardedSubcriteriaImpl ss = new ShardedSubcriteriaImpl(shards, parent);
        ss.uniqueResult();
        Assert.assertTrue(called[0]);
    }

    @Test
    public void testScrollDelegatesToParent() {
        List<Shard> shards = Lists.<Shard>newArrayList(new ShardDefaultMock());
        final boolean[] scrollNoArgsCalled = {false};
        final boolean[] scroll1ArgCalled = {false};
        ShardedCriteria parent = new ShardedCriteriaDefaultMock() {
            @Override
            public ScrollableResults scroll() throws HibernateException {
                scrollNoArgsCalled[0] = true;
                return null;
            }

            @Override
            public ScrollableResults scroll(ScrollMode scrollMode)
                    throws HibernateException {
                scroll1ArgCalled[0] = true;
                return null;
            }
        };
        ShardedSubcriteriaImpl ss = new ShardedSubcriteriaImpl(shards, parent);
        ss.scroll();
        Assert.assertTrue(scrollNoArgsCalled[0]);
        Assert.assertFalse(scroll1ArgCalled[0]);

        scrollNoArgsCalled[0] = false;
        ss.scroll(ScrollMode.FORWARD_ONLY);
        Assert.assertFalse(scrollNoArgsCalled[0]);
        Assert.assertTrue(scroll1ArgCalled[0]);
    }

    @Test
    public void testEstablishCriteria() {
        Shard shard = new ShardDefaultMock() {
            @Override
            public Set<ShardId> getShardIds() {
                return Sets.newHashSet(new ShardId(0));
            }
        };
        Shard someOtherShard = new ShardDefaultMock();
        List<Shard> shards = Lists.newArrayList(shard, someOtherShard);
        ShardedCriteria parent = new ShardedCriteriaDefaultMock();
        ShardedSubcriteriaImpl ss = new ShardedSubcriteriaImpl(shards, parent);
        ss.getShardToEventListMap().get(shard).add(new CriteriaEventDefaultMock());
        final Criteria subcritToReturn = new CriteriaDefaultMock();
        SubcriteriaFactory factory = new SubcriteriaFactoryDefaultMock() {
            @Override
            public Criteria createSubcriteria(Criteria parent,
                                              Iterable<CriteriaEvent> events) {
                return subcritToReturn;
            }
        };
        Criteria parentCrit = new CriteriaDefaultMock();
        ss.getSubcriteriaRegistrar(shard).establishSubcriteria(parentCrit, factory);
        ss.getSubcriteriaRegistrar(someOtherShard).establishSubcriteria(parentCrit, factory);
        Assert.assertTrue(ss.getShardToEventListMap().get(shard).isEmpty());
        Assert.assertTrue(ss.getShardToEventListMap().get(someOtherShard).isEmpty());
        Assert.assertSame(subcritToReturn, ss.getShardToCriteriaMap().get(shard));
        Assert.assertSame(subcritToReturn, ss.getShardToCriteriaMap().get(someOtherShard));
    }
}
