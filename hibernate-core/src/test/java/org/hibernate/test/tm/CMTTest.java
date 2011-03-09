/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.tm;

import javax.transaction.Transaction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.EntityMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Order;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.service.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.service.jta.platform.spi.JtaPlatform;

import org.junit.Test;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.common.jta.AtomikosDataSourceConnectionProvider;
import org.hibernate.test.common.jta.AtomikosJtaPlatform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * @author Gavin King
 */
public class CMTTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "tm/Item.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.getProperties().put( JtaPlatformInitiator.JTA_PLATFORM, AtomikosJtaPlatform.class.getName() );
		cfg.getProperties().put( Environment.CONNECTION_PROVIDER, AtomikosDataSourceConnectionProvider.class.getName() );
		cfg.setProperty( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class.getName() );
		cfg.setProperty( Environment.AUTO_CLOSE_SESSION, "true" );
		cfg.setProperty( Environment.FLUSH_BEFORE_COMPLETION, "true" );
		cfg.setProperty( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.AFTER_STATEMENT.toString() );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.DEFAULT_ENTITY_MODE, EntityMode.MAP.toString() );
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return "transactional";
	}

	@Test
	public void testConcurrent() throws Exception {
		sessionFactory().getStatistics().clear();
		assertNotNull( sessionFactory().getEntityPersister( "Item" ).getCacheAccessStrategy() );
		assertEquals( 0, sessionFactory().getStatistics().getEntityLoadCount() );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		sessionFactory().evictEntity( "Item" );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s1 = openSession();
		foo = ( Map ) s1.get( "Item", "Foo" );
		//foo.put("description", "a big red foo");
		//s1.flush();
		Transaction tx1 = sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s2 = openSession();
		foo = ( Map ) s2.get( "Item", "Foo" );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx1 );
		tx1.commit();

		sessionFactory().evictEntity( "Item" );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s1 = openSession();
		s1.createCriteria( "Item" ).list();
		//foo.put("description", "a big red foo");
		//s1.flush();
		tx1 = sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s2 = openSession();
		s2.createCriteria( "Item" ).list();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx1 );
		tx1.commit();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s2 = openSession();
		s2.createCriteria( "Item" ).list();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		assertEquals( 7, sessionFactory().getStatistics().getEntityLoadCount() );
		assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
		assertEquals( 3, sessionFactory().getStatistics().getQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getQueryCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getQueryCacheMissCount() );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
	}

	@Test
	public void testConcurrentCachedQueries() throws Exception {
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		synchronized ( this ) {
			wait( 1000 );
		}

		sessionFactory().getStatistics().clear();

		sessionFactory().evictEntity( "Item" );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s4 = openSession();
		Transaction tx4 = sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s1 = openSession();
		List r1 = s1.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r1.size(), 2 );
		Transaction tx1 = sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s2 = openSession();
		List r2 = s2.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r2.size(), 2 );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheHitCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheHitCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheMissCount(), 1 );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx1 );
		tx1.commit();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s3 = openSession();
		s3.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheHitCount(), 4 );
		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheHitCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheMissCount(), 1 );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx4 );
		List r4 = s4.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r4.size(), 2 );
		tx4.commit();

		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheHitCount(), 6 );
		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheHitCount(), 3 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheMissCount(), 1 );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
	}

	@Test
	@RequiresDialectFeature(
			value = DialectChecks.DoesReadCommittedNotCauseWritersToBlockReadersCheck.class,
			comment = "write locks block readers"
	)
	public void testConcurrentCachedDirtyQueries() throws Exception {
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		synchronized ( this ) {
			wait( 1000 );
		}

		sessionFactory().getStatistics().clear();

		sessionFactory().evictEntity( "Item" );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s4 = openSession();
		Transaction tx4 = sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s1 = openSession();
		List r1 = s1.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r1.size(), 2 );
		foo = ( Map ) r1.get( 0 );
		foo.put( "description", "a big red foo" );
		s1.flush();
		Transaction tx1 = sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s2 = openSession();
		List r2 = s2.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r2.size(), 2 );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheHitCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getEntityLoadCount(), 4 );
		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getQueryExecutionCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getQueryCachePutCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheMissCount(), 2 );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx1 );
		tx1.commit();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s3 = openSession();
		s3.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheHitCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getEntityLoadCount(), 6 );
		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getQueryExecutionCount(), 3 );
		assertEquals( sessionFactory().getStatistics().getQueryCachePutCount(), 3 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheMissCount(), 3 );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx4 );
		List r4 = s4.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r4.size(), 2 );
		tx4.commit();

		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheHitCount(), 2 );
		assertEquals( sessionFactory().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getEntityLoadCount(), 6 );
		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( sessionFactory().getStatistics().getQueryExecutionCount(), 3 );
		assertEquals( sessionFactory().getStatistics().getQueryCachePutCount(), 3 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheHitCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getQueryCacheMissCount(), 3 );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
	}

	@Test
	public void testCMT() throws Exception {
		sessionFactory().getStatistics().clear();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		assertFalse( s.isOpen() );

		assertEquals( sessionFactory().getStatistics().getFlushCount(), 0 );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().rollback();
		assertFalse( s.isOpen() );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		Map item = new HashMap();
		item.put( "name", "The Item" );
		item.put( "description", "The only item we have" );
		s.persist( "Item", item );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		assertFalse( s.isOpen() );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		item = ( Map ) s.createQuery( "from Item" ).uniqueResult();
		assertNotNull( item );
		s.delete( item );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		assertFalse( s.isOpen() );

		assertEquals( sessionFactory().getStatistics().getTransactionCount(), 4 );
		assertEquals( sessionFactory().getStatistics().getSuccessfulTransactionCount(), 3 );
		assertEquals( sessionFactory().getStatistics().getEntityDeleteCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getEntityInsertCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getSessionOpenCount(), 4 );
		assertEquals( sessionFactory().getStatistics().getSessionCloseCount(), 4 );
		assertEquals( sessionFactory().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( sessionFactory().getStatistics().getFlushCount(), 2 );

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
	}

	@Test
	public void testCurrentSession() throws Exception {
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = sessionFactory().getCurrentSession();
		Session s2 = sessionFactory().getCurrentSession();
		assertSame( s, s2 );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		assertFalse( s.isOpen() );

		// TODO : would be nice to automate-test that the SF internal map actually gets cleaned up
		//      i verified that is does currently in my debugger...
	}

	@Test
	public void testCurrentSessionWithIterate() throws Exception {
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.persist( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.persist( "Item", item2 );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// First, test iterating the partial iterator; iterate to past
		// the first, but not the second, item
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		Iterator itr = s.createQuery( "from Item" ).iterate();
		if ( !itr.hasNext() ) {
			fail( "No results in iterator" );
		}
		itr.next();
		if ( !itr.hasNext() ) {
			fail( "Only one result in iterator" );
		}
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// Next, iterate the entire result
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		itr = s.createQuery( "from Item" ).iterate();
		if ( !itr.hasNext() ) {
			fail( "No results in iterator" );
		}
		while ( itr.hasNext() ) {
			itr.next();
		}
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
	}

	@Test
	public void testCurrentSessionWithScroll() throws Exception {
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = sessionFactory().getCurrentSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.persist( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.persist( "Item", item2 );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// First, test partially scrolling the result with out closing
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		ScrollableResults results = s.createQuery( "from Item" ).scroll();
		results.next();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// Next, test partially scrolling the result with closing
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		results.next();
		results.close();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// Next, scroll the entire result (w/o closing)
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		while ( results.next() ) {
			// do nothing
		}
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// Next, scroll the entire result (closing)
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		while ( results.next() ) {
			// do nothing
		}
		results.close();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = sessionFactory().getCurrentSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
	}

	@Test
	public void testAggressiveReleaseWithConnectionRetreival() throws Exception {
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.save( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.save( "Item", item2 );
		sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		try {
			sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
			s = sessionFactory().getCurrentSession();
			s.createQuery( "from Item" ).scroll().next();
			s.connection();
			sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		}
		finally {
			sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
			s = openSession();
			s.createQuery( "delete from Item" ).executeUpdate();
			sessionFactory().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		}
	}

}

