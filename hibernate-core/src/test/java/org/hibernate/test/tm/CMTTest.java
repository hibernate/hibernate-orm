/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.tm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.transaction.Transaction;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.EntityMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.criterion.Order;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * @author Gavin King
 */
@SkipForDialect(SQLServerDialect.class)
public class CMTTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "tm/Item.hbm.xml" };
	}

	@Override
	protected void addSettings(Map settings) {
		TestingJtaBootstrap.prepare( settings );
		//settings.put( AvailableSettings.TRANSACTION_STRATEGY, CMTTransactionFactory.class.getName() );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, JtaTransactionCoordinatorBuilderImpl.class.getName() );
		settings.put( AvailableSettings.AUTO_CLOSE_SESSION, "true" );
		settings.put( AvailableSettings.FLUSH_BEFORE_COMPLETION, "true" );
		settings.put( AvailableSettings.RELEASE_CONNECTIONS, ConnectionReleaseMode.AFTER_STATEMENT.toString() );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.USE_QUERY_CACHE, "true" );
		settings.put( AvailableSettings.CACHE_REGION_PREFIX, "" );
		settings.put( AvailableSettings.DEFAULT_ENTITY_MODE, EntityMode.MAP.toString() );
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return "transactional";
	}

	@Test
	public void testConcurrent() throws Exception {
		sessionFactory().getStatistics().clear();
		assertEquals( 0, sessionFactory().getStatistics().getUpdateTimestampsCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getUpdateTimestampsCachePutCount() );
		assertEquals( 0, sessionFactory().getStatistics().getUpdateTimestampsCacheMissCount() );
		assertNotNull( sessionFactory().getEntityPersister( "Item" ).getCacheAccessStrategy() );
		assertEquals( 0, sessionFactory().getStatistics().getEntityLoadCount() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		assertEquals(0, sessionFactory().getStatistics().getUpdateTimestampsCacheHitCount());
		assertEquals(2, sessionFactory().getStatistics().getUpdateTimestampsCachePutCount()); // One preinvalidate & one invalidate
		assertEquals(0, sessionFactory().getStatistics().getUpdateTimestampsCacheMissCount());

		sessionFactory().getCache().evictEntityRegion( "Item" );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s1 = openSession();
		foo = ( Map ) s1.get( "Item", "Foo" );
		//foo.put("description", "a big red foo");
		//s1.flush();
		Transaction tx = TestingJtaPlatformImpl.INSTANCE.getTransactionManager().suspend();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s2 = openSession();
		foo = ( Map ) s2.get( "Item", "Foo" );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().resume( tx );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		sessionFactory().getCache().evictEntityRegion( "Item" );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s1 = openSession();
		s1.createCriteria( "Item" ).list();
		//foo.put("description", "a big red foo");
		//s1.flush();
		tx = TestingJtaPlatformImpl.INSTANCE.getTransactionManager().suspend();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s2 = openSession();
		s2.createCriteria( "Item" ).list();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().resume( tx );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s2 = openSession();
		s2.createCriteria( "Item" ).list();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertEquals( 7, sessionFactory().getStatistics().getEntityLoadCount() );
		assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
		assertEquals( 3, sessionFactory().getStatistics().getQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getQueryCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getQueryCacheMissCount() );
		assertEquals( 0, sessionFactory().getStatistics().getUpdateTimestampsCacheHitCount() );
		assertEquals( 2, sessionFactory().getStatistics().getUpdateTimestampsCachePutCount() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	public void testConcurrentCachedQueries() throws Exception {
		sessionFactory().getStatistics().clear();
		cleanupCache();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		synchronized ( this ) {
			wait( 1000 );
		}

		sessionFactory().getStatistics().clear();

		sessionFactory().getCache().evictEntityRegion( "Item" );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s4 = openSession();
		Transaction tx4 = TestingJtaPlatformImpl.INSTANCE.getTransactionManager().suspend();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s1 = openSession();
		List r1 = s1.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r1.size(), 2 );
		Transaction tx1 = TestingJtaPlatformImpl.INSTANCE.getTransactionManager().suspend();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s2 = openSession();
		List r2 = s2.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r2.size(), 2 );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheHitCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheHitCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheMissCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getUpdateTimestampsCacheHitCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getUpdateTimestampsCachePutCount(), 0 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().resume( tx1 );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s3 = openSession();
		s3.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheHitCount(), 4 );
		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheHitCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheMissCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getUpdateTimestampsCacheHitCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getUpdateTimestampsCachePutCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getUpdateTimestampsCacheMissCount(), 0 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().resume( tx4 );
		List r4 = s4.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r4.size(), 2 );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheHitCount(), 6 );
		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheHitCount(), 3 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheMissCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getUpdateTimestampsCacheHitCount(), 3 );
		assertEquals( sessionFactory().getStatistics().getUpdateTimestampsCachePutCount(), 0 );


		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	@RequiresDialectFeature(
			value = DialectChecks.DoesReadCommittedNotCauseWritersToBlockReadersCheck.class,
			comment = "write locks block readers"
	)
	public void testConcurrentCachedDirtyQueries() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		synchronized ( this ) {
			wait( 1000 );
		}

		sessionFactory().getStatistics().clear();
		cleanupCache();  // we need a clean 2L cache here.

		// open a TX and suspend it
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s4 = openSession();
		Transaction tx4 = TestingJtaPlatformImpl.INSTANCE.getTransactionManager().suspend();

		// open a new TX and execute a query, this would fill the query cache.
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s1 = openSession();
		List r1 = s1.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r1.size(), 2 );
		foo = ( Map ) r1.get( 0 );
		// update data and make query cache stale, but TX is suspended
		foo.put( "description", "a big red foo" );
		s1.flush();
		Transaction tx1 = TestingJtaPlatformImpl.INSTANCE.getTransactionManager().suspend();

		// open a new TX and run query again
		// this TX is committed after query
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s2 = openSession();
		List r2 = s2.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r2.size(), 2 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertEquals( 0, sessionFactory().getStatistics().getSecondLevelCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getSecondLevelCacheMissCount() );
		assertEquals( 4, sessionFactory().getStatistics().getEntityLoadCount() );
		assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
		assertEquals( 2, sessionFactory().getStatistics().getQueryExecutionCount() );
		assertEquals( 2, sessionFactory().getStatistics().getQueryCachePutCount() );
		assertEquals( 0, sessionFactory().getStatistics().getQueryCacheHitCount() );
		assertEquals( 2, sessionFactory().getStatistics().getQueryCacheMissCount() );

		// updateTimestampsCache put happens at two places
		// 1. {@link org.hibernate.engine.spi.ActionQueue#registerCleanupActions} calls preinvalidate
		// 2. {@link org.hibernate.engine.spi.ActionQueue.AfterTransactionCompletionProcessQueue#afterTransactionCompletion} calls invalidate
		// but since the TX which the update action happened is not committed yet, so there should be only 1 updateTimestamps put.
		assertEquals( 1, sessionFactory().getStatistics().getUpdateTimestampsCachePutCount() );

		// updateTimestampsCache hit only happens when the query cache data's timestamp is newer
		// than the timestamp of when update happens
		// since there is only 1 update action
		assertEquals( 1, sessionFactory().getStatistics().getUpdateTimestampsCacheHitCount() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().resume( tx1 );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		// update action's TX committed, so, invalidate is called, put new timestamp into UpdateTimestampsCache
		assertEquals( 2, sessionFactory().getStatistics().getUpdateTimestampsCachePutCount() );
		// but no more query cache lookup here, so it should still 1
		assertEquals( 1, sessionFactory().getStatistics().getUpdateTimestampsCacheHitCount() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s3 = openSession();
		s3.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertEquals( 0, sessionFactory().getStatistics().getSecondLevelCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getSecondLevelCacheMissCount() );
		assertEquals( 6, sessionFactory().getStatistics().getEntityLoadCount() );
		assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
		assertEquals( 3, sessionFactory().getStatistics().getQueryExecutionCount() );
		assertEquals( 3, sessionFactory().getStatistics().getQueryCachePutCount() );
		assertEquals( 0, sessionFactory().getStatistics().getQueryCacheHitCount() );
		assertEquals( 3, sessionFactory().getStatistics().getQueryCacheMissCount() );
		// a new query cache hit and one more update timestamps cache hit, so should be 2
		assertEquals( 2, sessionFactory().getStatistics().getUpdateTimestampsCacheHitCount() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().resume( tx4 );
		List r4 = s4.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r4.size(), 2 );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		assertEquals( 2, sessionFactory().getStatistics().getSecondLevelCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getSecondLevelCacheMissCount() );
		assertEquals( 6, sessionFactory().getStatistics().getEntityLoadCount() );
		assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
		assertEquals( 3, sessionFactory().getStatistics().getQueryExecutionCount() );
		assertEquals( 3, sessionFactory().getStatistics().getQueryCachePutCount() );
		assertEquals( 1, sessionFactory().getStatistics().getQueryCacheHitCount() );
		assertEquals( 3, sessionFactory().getStatistics().getQueryCacheMissCount() );
		assertEquals( 3, sessionFactory().getStatistics().getUpdateTimestampsCacheHitCount() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	public void testCMT() throws Exception {
		sessionFactory().getStatistics().clear();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		assertFalse( s.isOpen() );

		assertEquals( sessionFactory().getStatistics().getFlushCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getEntityInsertCount(), 0 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
		assertFalse( s.isOpen() );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		Map item = new HashMap();
		item.put( "name", "The Item" );
		item.put( "description", "The only item we have" );
		s.persist( "Item", item );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		assertFalse( s.isOpen() );
		assertEquals( sessionFactory().getStatistics().getFlushCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getEntityInsertCount(), 1 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		item = ( Map ) s.createQuery( "from Item" ).uniqueResult();
		assertNotNull( item );
		s.delete( item );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		assertFalse( s.isOpen() );

		assertEquals( sessionFactory().getStatistics().getTransactionCount(), 4 );
		assertEquals( sessionFactory().getStatistics().getSuccessfulTransactionCount(), 3 );
		assertEquals( sessionFactory().getStatistics().getEntityDeleteCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getEntityInsertCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getSessionOpenCount(), 4 );
		assertEquals( sessionFactory().getStatistics().getSessionCloseCount(), 4 );
		assertEquals( sessionFactory().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getFlushCount(), 2 );

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	public void testCurrentSession() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = sessionFactory().getCurrentSession();
		Session s2 = sessionFactory().getCurrentSession();
		assertSame( s, s2 );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		assertFalse( s.isOpen() );

		// TODO : would be nice to automate-test that the SF internal map actually gets cleaned up
		//      i verified that is does currently in my debugger...
	}

	@Test
	public void testCurrentSessionWithIterate() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = openSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.persist( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.persist( "Item", item2 );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		// First, test iterating the partial iterator; iterate to past
		// the first, but not the second, item
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		Iterator itr = s.createQuery( "from Item" ).iterate();
		if ( !itr.hasNext() ) {
			fail( "No results in iterator" );
		}
		itr.next();
		if ( !itr.hasNext() ) {
			fail( "Only one result in iterator" );
		}
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		// Next, iterate the entire result
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		itr = s.createQuery( "from Item" ).iterate();
		if ( !itr.hasNext() ) {
			fail( "No results in iterator" );
		}
		while ( itr.hasNext() ) {
			itr.next();
		}
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	@Test
	public void testCurrentSessionWithScroll() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		Session s = sessionFactory().getCurrentSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.persist( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.persist( "Item", item2 );
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		// First, test partially scrolling the result with out closing
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		ScrollableResults results = s.createQuery( "from Item" ).scroll();
		results.next();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		// Next, test partially scrolling the result with closing
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		results.next();
		results.close();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		// Next, scroll the entire result (w/o closing)
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		while ( results.next() ) {
			// do nothing
		}
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		// Next, scroll the entire result (closing)
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		while ( results.next() ) {
			// do nothing
		}
		results.close();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	protected boolean rebuildSessionFactoryOnError() {
		return false;
	}

}

