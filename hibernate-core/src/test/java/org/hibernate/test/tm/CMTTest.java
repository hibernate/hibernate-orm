//$Id: CMTTest.java 11303 2007-03-19 22:06:14Z steve.ebersole@jboss.com $
package org.hibernate.test.tm;

import junit.framework.Test;

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
import org.hibernate.test.common.jta.AtomikosDataSourceConnectionProvider;
import org.hibernate.test.common.jta.AtomikosJtaPlatform;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

import javax.transaction.Transaction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Gavin King
 */
public class CMTTest extends FunctionalTestCase {

	public CMTTest(String str) {
		super( str );
	}

	public String[] getMappings() {
		return new String[] { "tm/Item.hbm.xml" };
	}

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

	public String getCacheConcurrencyStrategy() {
		return "transactional";
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CMTTest.class );
	}

	public void testConcurrent() throws Exception {
		getSessions().getStatistics().clear();
		assertNotNull( sfi().getEntityPersister( "Item" ).getCacheAccessStrategy() );
		assertEquals( 0, getSessions().getStatistics().getEntityLoadCount() );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		getSessions().evictEntity( "Item" );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s1 = openSession();
		foo = ( Map ) s1.get( "Item", "Foo" );
		//foo.put("description", "a big red foo");
		//s1.flush();
		Transaction tx1 = sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s2 = openSession();
		foo = ( Map ) s2.get( "Item", "Foo" );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx1 );
		tx1.commit();

		getSessions().evictEntity( "Item" );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s1 = openSession();
		s1.createCriteria( "Item" ).list();
		//foo.put("description", "a big red foo");
		//s1.flush();
		tx1 = sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s2 = openSession();
		s2.createCriteria( "Item" ).list();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx1 );
		tx1.commit();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s2 = openSession();
		s2.createCriteria( "Item" ).list();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		assertEquals( 7, getSessions().getStatistics().getEntityLoadCount() );
		assertEquals( 0, getSessions().getStatistics().getEntityFetchCount() );
		assertEquals( 3, getSessions().getStatistics().getQueryExecutionCount() );
		assertEquals( 0, getSessions().getStatistics().getQueryCacheHitCount() );
		assertEquals( 0, getSessions().getStatistics().getQueryCacheMissCount() );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
	}

	public void testConcurrentCachedQueries() throws Exception {

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		synchronized ( this ) {
			wait( 1000 );
		}

		getSessions().getStatistics().clear();

		getSessions().evictEntity( "Item" );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s4 = openSession();
		Transaction tx4 = sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s1 = openSession();
		List r1 = s1.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r1.size(), 2 );
		Transaction tx1 = sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s2 = openSession();
		List r2 = s2.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r2.size(), 2 );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 2 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 1 );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx1 );
		tx1.commit();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s3 = openSession();
		s3.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 4 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 2 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 1 );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx4 );
		List r4 = s4.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r4.size(), 2 );
		tx4.commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 6 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 3 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 1 );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
	}

	public void testConcurrentCachedDirtyQueries() throws Exception {
		if ( getDialect().doesReadCommittedCauseWritersToBlockReaders() ) {
			reportSkip( "write locks block readers", "concurrent queries" );
			return;
		}

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		synchronized ( this ) {
			wait( 1000 );
		}

		getSessions().getStatistics().clear();

		getSessions().evictEntity( "Item" );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s4 = openSession();
		Transaction tx4 = sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s1 = openSession();
		List r1 = s1.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r1.size(), 2 );
		foo = ( Map ) r1.get( 0 );
		foo.put( "description", "a big red foo" );
		s1.flush();
		Transaction tx1 = sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().suspend();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s2 = openSession();
		List r2 = s2.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r2.size(), 2 );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 4 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 2 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 2 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 2 );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx1 );
		tx1.commit();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s3 = openSession();
		s3.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 6 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 3 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 3 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 3 );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().resume( tx4 );
		List r4 = s4.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r4.size(), 2 );
		tx4.commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 2 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 6 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 3 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 3 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 3 );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
	}

	public void testCMT() throws Exception {
		getSessions().getStatistics().clear();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		assertFalse( s.isOpen() );

		assertEquals( getSessions().getStatistics().getFlushCount(), 0 );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().rollback();
		assertFalse( s.isOpen() );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		Map item = new HashMap();
		item.put( "name", "The Item" );
		item.put( "description", "The only item we have" );
		s.persist( "Item", item );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		assertFalse( s.isOpen() );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		item = ( Map ) s.createQuery( "from Item" ).uniqueResult();
		assertNotNull( item );
		s.delete( item );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		assertFalse( s.isOpen() );

		assertEquals( getSessions().getStatistics().getTransactionCount(), 4 );
		assertEquals( getSessions().getStatistics().getSuccessfulTransactionCount(), 3 );
		assertEquals( getSessions().getStatistics().getEntityDeleteCount(), 1 );
		assertEquals( getSessions().getStatistics().getEntityInsertCount(), 1 );
		assertEquals( getSessions().getStatistics().getSessionOpenCount(), 4 );
		assertEquals( getSessions().getStatistics().getSessionCloseCount(), 4 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getFlushCount(), 2 );

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

	}

	public void testCurrentSession() throws Exception {
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = getSessions().getCurrentSession();
		Session s2 = getSessions().getCurrentSession();
		assertSame( s, s2 );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		assertFalse( s.isOpen() );

		// TODO : would be nice to automate-test that the SF internal map actually gets cleaned up
		//      i verified that is does currently in my debugger...
	}

	public void testCurrentSessionWithIterate() throws Exception {
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.persist( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.persist( "Item", item2 );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// First, test iterating the partial iterator; iterate to past
		// the first, but not the second, item
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = getSessions().getCurrentSession();
		Iterator itr = s.createQuery( "from Item" ).iterate();
		if ( !itr.hasNext() ) {
			fail( "No results in iterator" );
		}
		itr.next();
		if ( !itr.hasNext() ) {
			fail( "Only one result in iterator" );
		}
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// Next, iterate the entire result
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = getSessions().getCurrentSession();
		itr = s.createQuery( "from Item" ).iterate();
		if ( !itr.hasNext() ) {
			fail( "No results in iterator" );
		}
		while ( itr.hasNext() ) {
			itr.next();
		}
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
	}

	public void testCurrentSessionWithScroll() throws Exception {
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = getSessions().getCurrentSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.persist( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.persist( "Item", item2 );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// First, test partially scrolling the result with out closing
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = getSessions().getCurrentSession();
		ScrollableResults results = s.createQuery( "from Item" ).scroll();
		results.next();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// Next, test partially scrolling the result with closing
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = getSessions().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		results.next();
		results.close();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// Next, scroll the entire result (w/o closing)
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = getSessions().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		while ( results.next() ) {
			// do nothing
		}
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		// Next, scroll the entire result (closing)
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = getSessions().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		while ( results.next() ) {
			// do nothing
		}
		results.close();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		s = getSessions().getCurrentSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
	}

	public void testAggressiveReleaseWithConnectionRetreival() throws Exception {
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
		Session s = openSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.save( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.save( "Item", item2 );
		sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();

		try {
			sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
			s = getSessions().getCurrentSession();
			s.createQuery( "from Item" ).scroll().next();
			s.connection();
			sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		}
		finally {
			sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().begin();
			s = openSession();
			s.createQuery( "delete from Item" ).executeUpdate();
			sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager().commit();
		}
	}

}

