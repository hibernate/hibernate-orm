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

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.shards.criteria.CriteriaEvent;
import org.hibernate.shards.criteria.CriteriaFactory;
import org.hibernate.shards.criteria.CriteriaFactoryDefaultMock;
import org.hibernate.shards.criteria.CriteriaId;
import org.hibernate.shards.defaultmock.CriteriaDefaultMock;
import org.hibernate.shards.defaultmock.CriteriaEventDefaultMock;
import org.hibernate.shards.defaultmock.InterceptorDefaultMock;
import org.hibernate.shards.defaultmock.QueryDefaultMock;
import org.hibernate.shards.defaultmock.SessionDefaultMock;
import org.hibernate.shards.defaultmock.SessionFactoryDefaultMock;
import org.hibernate.shards.defaultmock.ShardedCriteriaDefaultMock;
import org.hibernate.shards.query.QueryEvent;
import org.hibernate.shards.query.QueryEventDefaultMock;
import org.hibernate.shards.query.QueryFactory;
import org.hibernate.shards.query.QueryFactoryDefaultMock;
import org.hibernate.shards.query.QueryId;
import org.hibernate.shards.query.ShardedQueryDefaultMock;
import org.hibernate.shards.session.OpenSessionEvent;
import org.hibernate.shards.session.OpenSessionEventDefaultMock;
import org.hibernate.shards.util.Sets;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ShardImplTest {

    @Test
    public void testAddOpenSessionEvent() {
        ShardImpl shard = new ShardImpl(new ShardId(1), new SessionFactoryDefaultMock());
        try {
            shard.addOpenSessionEvent(null);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }
        OpenSessionEvent ose = new OpenSessionEventDefaultMock();
        shard.addOpenSessionEvent(ose);
        Assert.assertNotNull(shard.getOpenSessionEvents());
        Assert.assertEquals(1, shard.getOpenSessionEvents().size());
        Assert.assertSame(ose, shard.getOpenSessionEvents().get(0));

        // now add another and make sure it is added to the end
        OpenSessionEvent anotherOse = new OpenSessionEventDefaultMock();
        shard.addOpenSessionEvent(anotherOse);
        Assert.assertNotNull(shard.getOpenSessionEvents());
        Assert.assertEquals(2, shard.getOpenSessionEvents().size());
        Assert.assertSame(ose, shard.getOpenSessionEvents().get(0));
        Assert.assertSame(anotherOse, shard.getOpenSessionEvents().get(1));
    }

    @Test
    public void testAddCriteriaEvent() {
        ShardImpl shard = new ShardImpl(new ShardId(1), new SessionFactoryDefaultMock());
        try {
            shard.addCriteriaEvent(null, null);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        CriteriaId criteriaId = new CriteriaId(2);
        try {
            shard.addCriteriaEvent(criteriaId, null);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        CriteriaEvent ce = new CriteriaEventDefaultMock();
        try {
            shard.addCriteriaEvent(null, ce);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        shard.addCriteriaEvent(criteriaId, ce);
        Assert.assertNotNull(shard.getCriteriaEventMap());
        Assert.assertEquals(1, shard.getCriteriaEventMap().size());
        Assert.assertEquals(1, shard.getCriteriaEventMap().get(criteriaId).size());
        Assert.assertSame(ce, shard.getCriteriaEventMap().get(criteriaId).get(0));

        // now add another event to the same criteria
        CriteriaEvent anotherCe = new CriteriaEventDefaultMock();
        shard.addCriteriaEvent(criteriaId, anotherCe);
        Assert.assertNotNull(shard.getCriteriaEventMap());
        Assert.assertEquals(1, shard.getCriteriaEventMap().size());
        Assert.assertEquals(2, shard.getCriteriaEventMap().get(criteriaId).size());
        Assert.assertSame(ce, shard.getCriteriaEventMap().get(criteriaId).get(0));
        Assert.assertSame(anotherCe, shard.getCriteriaEventMap().get(criteriaId).get(1));

        // now add an event to a different criteria
        CriteriaId anotherCriteriaId = new CriteriaId(3);
        CriteriaEvent yetAnotherCe = new CriteriaEventDefaultMock();
        shard.addCriteriaEvent(anotherCriteriaId, yetAnotherCe);
        Assert.assertNotNull(shard.getCriteriaEventMap());
        Assert.assertEquals(2, shard.getCriteriaEventMap().size());
        Assert.assertEquals(2, shard.getCriteriaEventMap().get(criteriaId).size());
        Assert.assertSame(ce, shard.getCriteriaEventMap().get(criteriaId).get(0));
        Assert.assertSame(anotherCe, shard.getCriteriaEventMap().get(criteriaId).get(1));
        Assert.assertEquals(1, shard.getCriteriaEventMap().get(anotherCriteriaId).size());
        Assert.assertSame(yetAnotherCe, shard.getCriteriaEventMap().get(anotherCriteriaId).get(0));
    }

    @Test
    public void testEstablishSessionNoEvents() {
        MySessionFactory sf = new MySessionFactory();
        ShardImpl shardImpl = new ShardImpl(new ShardId(1), sf);
        shardImpl.establishSession();
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        shardImpl.establishSession();
        Assert.assertEquals(1, sf.numOpenSessionCalls);
    }

    @Test
    public void testEstablishSessionNoEventsWithInterceptor() {
        final MySessionFactory sf = new MySessionFactory();
        final Interceptor interceptor = new InterceptorDefaultMock();
        final ShardImpl shardImpl = new ShardImpl(Sets.newHashSet(new ShardId(1)), sf, interceptor);
        shardImpl.establishSession();
        Assert.assertEquals(1, sf.numOpenSessionWithInterceptorCalls);
        shardImpl.establishSession();
        Assert.assertEquals(1, sf.numOpenSessionWithInterceptorCalls);
    }

    @Test
    public void testEstablishSessionWithEvents() {
        MySessionFactory sf = new MySessionFactory();
        ShardImpl shardImpl = new ShardImpl(new ShardId(1), sf);
        MyOpenSessionEvent event1 = new MyOpenSessionEvent();
        MyOpenSessionEvent event2 = new MyOpenSessionEvent();
        shardImpl.addOpenSessionEvent(event1);
        shardImpl.addOpenSessionEvent(event2);
        shardImpl.establishSession();
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertEquals(1, event1.numOnOpenSessionCalls);
        Assert.assertEquals(1, event2.numOnOpenSessionCalls);
        Assert.assertTrue(shardImpl.getOpenSessionEvents().isEmpty());
        shardImpl.establishSession();
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertEquals(1, event1.numOnOpenSessionCalls);
        Assert.assertEquals(1, event2.numOnOpenSessionCalls);
        Assert.assertTrue(shardImpl.getOpenSessionEvents().isEmpty());
    }

    @Test
    public void testEstablishCriteriaNoEvents() {
        MySessionFactory sf = new MySessionFactory();
        ShardImpl shardImpl = new ShardImpl(new ShardId(1), sf);
        CriteriaId critId = new CriteriaId(3);
        Criteria crit = new CriteriaDefaultMock();
        MyCriteriaFactory mcf = new MyCriteriaFactory(crit);
        MyShardedCriteria msc = new MyShardedCriteria(critId, mcf);
        shardImpl.establishCriteria(msc);
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertNotNull(mcf.createCriteriaCalledWith);
        Assert.assertEquals(1, shardImpl.getCriteriaMap().size());
        Assert.assertSame(crit, shardImpl.getCriteriaMap().get(critId));

        shardImpl.establishCriteria(msc);
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertNotNull(mcf.createCriteriaCalledWith);
        Assert.assertEquals(1, shardImpl.getCriteriaMap().size());
        Assert.assertSame(crit, shardImpl.getCriteriaMap().get(critId));
    }

    @Test
    public void testEstablishCriteriaWithEvents() {
        MySessionFactory sf = new MySessionFactory();
        ShardImpl shardImpl = new ShardImpl(new ShardId(1), sf);
        MyCriteriaEvent event1 = new MyCriteriaEvent();
        MyCriteriaEvent event2 = new MyCriteriaEvent();
        CriteriaId critId = new CriteriaId(3);
        shardImpl.addCriteriaEvent(critId, event1);
        shardImpl.addCriteriaEvent(critId, event2);
        Criteria crit = new CriteriaDefaultMock();
        MyCriteriaFactory mcf = new MyCriteriaFactory(crit);
        MyShardedCriteria msc = new MyShardedCriteria(critId, mcf);
        shardImpl.establishCriteria(msc);
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertNotNull(mcf.createCriteriaCalledWith);
        Assert.assertEquals(1, shardImpl.getCriteriaMap().size());
        Assert.assertSame(crit, shardImpl.getCriteriaMap().get(critId));
        Assert.assertEquals(1, event1.numOnEventCalls);
        Assert.assertEquals(1, event2.numOnEventCalls);
        Assert.assertTrue(shardImpl.getCriteriaEventMap().get(critId).isEmpty());

        shardImpl.establishCriteria(msc);
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertNotNull(mcf.createCriteriaCalledWith);
        Assert.assertEquals(1, shardImpl.getCriteriaMap().size());
        Assert.assertSame(crit, shardImpl.getCriteriaMap().get(critId));
        Assert.assertEquals(1, event1.numOnEventCalls);
        Assert.assertEquals(1, event2.numOnEventCalls);
        Assert.assertTrue(shardImpl.getCriteriaEventMap().get(critId).isEmpty());
    }

    @Test
    public void testEstablishMultipleCriteria() {
        MySessionFactory sf = new MySessionFactory();
        ShardImpl shardImpl = new ShardImpl(new ShardId(1), sf);

        MyCriteriaEvent event1 = new MyCriteriaEvent();
        MyCriteriaEvent event2 = new MyCriteriaEvent();
        CriteriaId critId1 = new CriteriaId(3);
        Criteria crit1 = new CriteriaDefaultMock();
        MyCriteriaFactory mcf1 = new MyCriteriaFactory(crit1);
        MyShardedCriteria msc1 = new MyShardedCriteria(critId1, mcf1);

        shardImpl.addCriteriaEvent(critId1, event1);
        shardImpl.addCriteriaEvent(critId1, event2);

        CriteriaId critId2 = new CriteriaId(4);
        Criteria crit2 = new CriteriaDefaultMock();
        MyCriteriaFactory mcf2 = new MyCriteriaFactory(crit2);
        MyShardedCriteria msc2 = new MyShardedCriteria(critId2, mcf2);

        shardImpl.establishCriteria(msc1);
        shardImpl.establishCriteria(msc2);

        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertEquals(2, shardImpl.getCriteriaMap().size());

        Assert.assertNotNull(mcf1.createCriteriaCalledWith);
        Assert.assertSame(crit1, shardImpl.getCriteriaMap().get(critId1));
        Assert.assertEquals(1, event1.numOnEventCalls);
        Assert.assertEquals(1, event2.numOnEventCalls);
        Assert.assertTrue(shardImpl.getCriteriaEventMap().get(critId1).isEmpty());

        Assert.assertNotNull(mcf2.createCriteriaCalledWith);
        Assert.assertSame(crit2, shardImpl.getCriteriaMap().get(critId2));
        Assert.assertNull(shardImpl.getCriteriaEventMap().get(critId2));

        shardImpl.establishCriteria(msc1);
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertNotNull(mcf1.createCriteriaCalledWith);
        Assert.assertEquals(2, shardImpl.getCriteriaMap().size());
        Assert.assertSame(crit1, shardImpl.getCriteriaMap().get(critId1));
        Assert.assertEquals(1, event1.numOnEventCalls);
        Assert.assertEquals(1, event2.numOnEventCalls);

        Assert.assertNotNull(mcf2.createCriteriaCalledWith);
        Assert.assertSame(crit2, shardImpl.getCriteriaMap().get(critId2));
        Assert.assertNull(shardImpl.getCriteriaEventMap().get(critId2));

        Assert.assertTrue(shardImpl.getCriteriaEventMap().get(critId1).isEmpty());
    }

    @Test
    public void testAddQueryEvent() throws Exception {
        ShardImpl shard = new ShardImpl(new ShardId(1), new SessionFactoryDefaultMock());
        try {
            shard.addQueryEvent(null, null);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        QueryId queryId = new QueryId(1);
        try {
            shard.addQueryEvent(queryId, null);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        QueryEvent qe = new QueryEventDefaultMock();
        try {
            shard.addQueryEvent(null, qe);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        shard.addQueryEvent(queryId, qe);
        Assert.assertNotNull(shard.getQueryEventMap());
        Assert.assertEquals(1, shard.getQueryEventMap().size());
        Assert.assertEquals(1, shard.getQueryEventMap().get(queryId).size());
        Assert.assertSame(qe, shard.getQueryEventMap().get(queryId).get(0));

        // now add another event to the same query
        QueryEvent anotherQe = new QueryEventDefaultMock();
        shard.addQueryEvent(queryId, anotherQe);
        Assert.assertNotNull(shard.getQueryEventMap());
        Assert.assertEquals(1, shard.getQueryEventMap().size());
        Assert.assertEquals(2, shard.getQueryEventMap().get(queryId).size());
        Assert.assertSame(qe, shard.getQueryEventMap().get(queryId).get(0));
        Assert.assertSame(anotherQe, shard.getQueryEventMap().get(queryId).get(1));

        // now add an event to a different query
        QueryId anotherQueryId = new QueryId(3);
        QueryEvent yetAnotherQe = new QueryEventDefaultMock();
        shard.addQueryEvent(anotherQueryId, yetAnotherQe);
        Assert.assertNotNull(shard.getQueryEventMap());
        Assert.assertEquals(2, shard.getQueryEventMap().size());
        Assert.assertEquals(2, shard.getQueryEventMap().get(queryId).size());
        Assert.assertSame(qe, shard.getQueryEventMap().get(queryId).get(0));
        Assert.assertSame(anotherQe, shard.getQueryEventMap().get(queryId).get(1));
        Assert.assertEquals(1, shard.getQueryEventMap().get(anotherQueryId).size());
        Assert.assertSame(yetAnotherQe, shard.getQueryEventMap().get(anotherQueryId).get(0));
    }

    @Test
    public void testEstablishQueryNoEvents() {
        MySessionFactory sf = new MySessionFactory();
        ShardImpl shardImpl = new ShardImpl(new ShardId(1), sf);
        QueryId critId = new QueryId(3);
        Query query = new QueryDefaultMock();
        MyQueryFactory mqf = new MyQueryFactory(query);
        MyShardedQuery msq = new MyShardedQuery(critId, mqf);
        shardImpl.establishQuery(msq);
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertNotNull(mqf.createQueryCalledWith);
        Assert.assertEquals(1, shardImpl.getQueryMap().size());
        Assert.assertSame(query, shardImpl.getQueryMap().get(critId));

        shardImpl.establishQuery(msq);
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertNotNull(mqf.createQueryCalledWith);
        Assert.assertEquals(1, shardImpl.getQueryMap().size());
        Assert.assertSame(query, shardImpl.getQueryMap().get(critId));
    }

    @Test
    public void testEstablishQueryWithEvents() {
        MySessionFactory sf = new MySessionFactory();
        ShardImpl shardImpl = new ShardImpl(new ShardId(1), sf);
        MyQueryEvent event1 = new MyQueryEvent();
        MyQueryEvent event2 = new MyQueryEvent();
        QueryId queryId = new QueryId(3);
        shardImpl.addQueryEvent(queryId, event1);
        shardImpl.addQueryEvent(queryId, event2);
        Query query = new QueryDefaultMock();
        MyQueryFactory mqf = new MyQueryFactory(query);
        MyShardedQuery msq = new MyShardedQuery(queryId, mqf);
        shardImpl.establishQuery(msq);
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertNotNull(mqf.createQueryCalledWith);
        Assert.assertEquals(1, shardImpl.getQueryMap().size());
        Assert.assertSame(query, shardImpl.getQueryMap().get(queryId));
        Assert.assertEquals(1, event1.numOnEventCalls);
        Assert.assertEquals(1, event2.numOnEventCalls);
        Assert.assertTrue(shardImpl.getQueryEventMap().get(queryId).isEmpty());

        shardImpl.establishQuery(msq);
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertNotNull(mqf.createQueryCalledWith);
        Assert.assertEquals(1, shardImpl.getQueryMap().size());
        Assert.assertSame(query, shardImpl.getQueryMap().get(queryId));
        Assert.assertEquals(1, event1.numOnEventCalls);
        Assert.assertEquals(1, event2.numOnEventCalls);
        Assert.assertTrue(shardImpl.getQueryEventMap().get(queryId).isEmpty());
    }

    @Test
    public void testEstablishMultipleQuery() {
        MySessionFactory sf = new MySessionFactory();
        ShardImpl shardImpl = new ShardImpl(new ShardId(1), sf);

        MyQueryEvent event1 = new MyQueryEvent();
        MyQueryEvent event2 = new MyQueryEvent();
        QueryId queryId1 = new QueryId(3);
        Query query1 = new QueryDefaultMock();
        MyQueryFactory mqf1 = new MyQueryFactory(query1);
        MyShardedQuery msq1 = new MyShardedQuery(queryId1, mqf1);

        shardImpl.addQueryEvent(queryId1, event1);
        shardImpl.addQueryEvent(queryId1, event2);

        QueryId queryId2 = new QueryId(4);
        Query query2 = new QueryDefaultMock();
        MyQueryFactory mqf2 = new MyQueryFactory(query2);
        MyShardedQuery msq2 = new MyShardedQuery(queryId2, mqf2);

        shardImpl.establishQuery(msq1);
        shardImpl.establishQuery(msq2);

        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertEquals(2, shardImpl.getQueryMap().size());

        Assert.assertNotNull(mqf1.createQueryCalledWith);
        Assert.assertSame(query1, shardImpl.getQueryMap().get(queryId1));
        Assert.assertEquals(1, event1.numOnEventCalls);
        Assert.assertEquals(1, event2.numOnEventCalls);
        Assert.assertTrue(shardImpl.getQueryEventMap().get(queryId1).isEmpty());

        Assert.assertNotNull(mqf2.createQueryCalledWith);
        Assert.assertSame(query2, shardImpl.getQueryMap().get(queryId2));
        Assert.assertNull(shardImpl.getQueryEventMap().get(queryId2));

        shardImpl.establishQuery(msq1);
        Assert.assertEquals(1, sf.numOpenSessionCalls);
        Assert.assertNotNull(mqf1.createQueryCalledWith);
        Assert.assertEquals(2, shardImpl.getQueryMap().size());
        Assert.assertSame(query1, shardImpl.getQueryMap().get(queryId1));
        Assert.assertEquals(1, event1.numOnEventCalls);
        Assert.assertEquals(1, event2.numOnEventCalls);

        Assert.assertNotNull(mqf2.createQueryCalledWith);
        Assert.assertSame(query2, shardImpl.getQueryMap().get(queryId2));
        Assert.assertNull(shardImpl.getQueryEventMap().get(queryId2));

        Assert.assertTrue(shardImpl.getQueryEventMap().get(queryId1).isEmpty());
    }

    private static final class MyOpenSessionEvent extends OpenSessionEventDefaultMock {
        private int numOnOpenSessionCalls = 0;

        @Override
        public void onOpenSession(org.hibernate.Session session) {
            numOnOpenSessionCalls++;
        }
    }

    private static final class MySessionFactory extends SessionFactoryDefaultMock {
        private int numOpenSessionCalls;
        private int numOpenSessionWithInterceptorCalls;

        @Override
        public org.hibernate.Session openSession() throws HibernateException {
            numOpenSessionCalls++;
            return new SessionDefaultMock();
        }
    }

    private static final class MyShardedCriteria extends ShardedCriteriaDefaultMock {
        private final CriteriaId critId;
        private final CriteriaFactory critFactory;

        public MyShardedCriteria(CriteriaId critId, CriteriaFactory critFactory) {
            this.critId = critId;
            this.critFactory = critFactory;
        }

        @Override
        public CriteriaId getCriteriaId() {
            return critId;
        }

        @Override
        public CriteriaFactory getCriteriaFactory() {
            return critFactory;
        }
    }

    private static final class MyCriteriaFactory extends CriteriaFactoryDefaultMock {
        private org.hibernate.Session createCriteriaCalledWith;
        private Criteria critToReturn;

        public MyCriteriaFactory(Criteria critToReturn) {
            this.critToReturn = critToReturn;
        }

        @Override
        public Criteria createCriteria(org.hibernate.Session session) {
            createCriteriaCalledWith = session;
            return critToReturn;
        }
    }

    private static final class MyCriteriaEvent implements CriteriaEvent {
        private int numOnEventCalls;

        public void onEvent(Criteria crit) {
            numOnEventCalls++;
        }
    }

    private static final class MyShardedQuery extends ShardedQueryDefaultMock {
        private final QueryId queryId;
        private final QueryFactory queryFactory;

        public MyShardedQuery(QueryId queryId, QueryFactory queryFactory) {
            this.queryId = queryId;
            this.queryFactory = queryFactory;
        }

        @Override
        public QueryId getQueryId() {
            return queryId;
        }

        @Override
        public QueryFactory getQueryFactory() {
            return queryFactory;
        }
    }

    public static final class MyQueryFactory extends QueryFactoryDefaultMock {
        private org.hibernate.Session createQueryCalledWith;
        private Query queryToReturn;

        public MyQueryFactory(Query queryToReturn) {
            this.queryToReturn = queryToReturn;
        }

        public Query createQuery(Session session) {
            createQueryCalledWith = session;
            return queryToReturn;
        }
    }

    private static final class MyQueryEvent implements QueryEvent {
        private int numOnEventCalls;

        public void onEvent(Query query) {
            numOnEventCalls++;
        }
    }
}
