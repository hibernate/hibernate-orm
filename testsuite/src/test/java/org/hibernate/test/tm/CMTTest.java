//$Id: CMTTest.java 11303 2007-03-19 22:06:14Z steve.ebersole@jboss.com $
package org.hibernate.test.tm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.transaction.Transaction;

import junit.framework.Test;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.EntityMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Order;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.transaction.CMTTransactionFactory;
import org.hibernate.util.SerializationHelper;

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
		cfg.setProperty( Environment.CONNECTION_PROVIDER, DummyConnectionProvider.class.getName() );
		cfg.setProperty( Environment.TRANSACTION_MANAGER_STRATEGY, DummyTransactionManagerLookup.class.getName() );
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

		DummyTransactionManager.INSTANCE.begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		DummyTransactionManager.INSTANCE.commit();

		getSessions().evictEntity( "Item" );

		DummyTransactionManager.INSTANCE.begin();
		Session s1 = openSession();
		foo = ( Map ) s1.get( "Item", "Foo" );
		//foo.put("description", "a big red foo");
		//s1.flush();
		Transaction tx1 = DummyTransactionManager.INSTANCE.suspend();

		DummyTransactionManager.INSTANCE.begin();
		Session s2 = openSession();
		foo = ( Map ) s2.get( "Item", "Foo" );
		DummyTransactionManager.INSTANCE.commit();

		DummyTransactionManager.INSTANCE.resume( tx1 );
		tx1.commit();

		getSessions().evictEntity( "Item" );

		DummyTransactionManager.INSTANCE.begin();
		s1 = openSession();
		s1.createCriteria( "Item" ).list();
		//foo.put("description", "a big red foo");
		//s1.flush();
		tx1 = DummyTransactionManager.INSTANCE.suspend();

		DummyTransactionManager.INSTANCE.begin();
		s2 = openSession();
		s2.createCriteria( "Item" ).list();
		DummyTransactionManager.INSTANCE.commit();

		DummyTransactionManager.INSTANCE.resume( tx1 );
		tx1.commit();

		DummyTransactionManager.INSTANCE.begin();
		s2 = openSession();
		s2.createCriteria( "Item" ).list();
		DummyTransactionManager.INSTANCE.commit();

		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 7 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 3 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 0 );

		DummyTransactionManager.INSTANCE.begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		DummyTransactionManager.INSTANCE.commit();
	}

	public void testConcurrentCachedQueries() throws Exception {

		DummyTransactionManager.INSTANCE.begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		DummyTransactionManager.INSTANCE.commit();

		synchronized ( this ) {
			wait( 1000 );
		}

		getSessions().getStatistics().clear();

		getSessions().evictEntity( "Item" );

		DummyTransactionManager.INSTANCE.begin();
		Session s4 = openSession();
		Transaction tx4 = DummyTransactionManager.INSTANCE.suspend();

		DummyTransactionManager.INSTANCE.begin();
		Session s1 = openSession();
		List r1 = s1.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r1.size(), 2 );
		Transaction tx1 = DummyTransactionManager.INSTANCE.suspend();

		DummyTransactionManager.INSTANCE.begin();
		Session s2 = openSession();
		List r2 = s2.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r2.size(), 2 );
		DummyTransactionManager.INSTANCE.commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 2 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 1 );

		DummyTransactionManager.INSTANCE.resume( tx1 );
		tx1.commit();

		DummyTransactionManager.INSTANCE.begin();
		Session s3 = openSession();
		s3.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		DummyTransactionManager.INSTANCE.commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 4 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 2 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 1 );

		DummyTransactionManager.INSTANCE.resume( tx4 );
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

		DummyTransactionManager.INSTANCE.begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		DummyTransactionManager.INSTANCE.commit();
	}

	public void testConcurrentCachedDirtyQueries() throws Exception {
		if ( getDialect().doesReadCommittedCauseWritersToBlockReaders() ) {
			reportSkip( "write locks block readers", "concurrent queries" );
			return;
		}

		DummyTransactionManager.INSTANCE.begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		DummyTransactionManager.INSTANCE.commit();

		synchronized ( this ) {
			wait( 1000 );
		}

		getSessions().getStatistics().clear();

		getSessions().evictEntity( "Item" );

		DummyTransactionManager.INSTANCE.begin();
		Session s4 = openSession();
		Transaction tx4 = DummyTransactionManager.INSTANCE.suspend();

		DummyTransactionManager.INSTANCE.begin();
		Session s1 = openSession();
		List r1 = s1.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r1.size(), 2 );
		foo = ( Map ) r1.get( 0 );
		foo.put( "description", "a big red foo" );
		s1.flush();
		Transaction tx1 = DummyTransactionManager.INSTANCE.suspend();

		DummyTransactionManager.INSTANCE.begin();
		Session s2 = openSession();
		List r2 = s2.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r2.size(), 2 );
		DummyTransactionManager.INSTANCE.commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 4 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 2 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 2 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 2 );

		DummyTransactionManager.INSTANCE.resume( tx1 );
		tx1.commit();

		DummyTransactionManager.INSTANCE.begin();
		Session s3 = openSession();
		s3.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		DummyTransactionManager.INSTANCE.commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 6 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 3 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 3 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 3 );

		DummyTransactionManager.INSTANCE.resume( tx4 );
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

		DummyTransactionManager.INSTANCE.begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		DummyTransactionManager.INSTANCE.commit();
	}

	public void testCMT() throws Exception {
		getSessions().getStatistics().clear();

		DummyTransactionManager.INSTANCE.begin();
		Session s = openSession();
		DummyTransactionManager.INSTANCE.getTransaction().commit();
		assertFalse( s.isOpen() );

		assertEquals( getSessions().getStatistics().getFlushCount(), 0 );

		DummyTransactionManager.INSTANCE.begin();
		s = openSession();
		DummyTransactionManager.INSTANCE.getTransaction().rollback();
		assertFalse( s.isOpen() );

		DummyTransactionManager.INSTANCE.begin();
		s = openSession();
		Map item = new HashMap();
		item.put( "name", "The Item" );
		item.put( "description", "The only item we have" );
		s.persist( "Item", item );
		DummyTransactionManager.INSTANCE.getTransaction().commit();
		assertFalse( s.isOpen() );

		DummyTransactionManager.INSTANCE.begin();
		s = openSession();
		item = ( Map ) s.createQuery( "from Item" ).uniqueResult();
		assertNotNull( item );
		s.delete( item );
		DummyTransactionManager.INSTANCE.getTransaction().commit();
		assertFalse( s.isOpen() );

		assertEquals( getSessions().getStatistics().getTransactionCount(), 4 );
		assertEquals( getSessions().getStatistics().getSuccessfulTransactionCount(), 3 );
		assertEquals( getSessions().getStatistics().getEntityDeleteCount(), 1 );
		assertEquals( getSessions().getStatistics().getEntityInsertCount(), 1 );
		assertEquals( getSessions().getStatistics().getSessionOpenCount(), 4 );
		assertEquals( getSessions().getStatistics().getSessionCloseCount(), 4 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getFlushCount(), 2 );

		DummyTransactionManager.INSTANCE.begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		DummyTransactionManager.INSTANCE.commit();

	}

	public void testCurrentSession() throws Exception {
		DummyTransactionManager.INSTANCE.begin();
		Session s = getSessions().getCurrentSession();
		Session s2 = getSessions().getCurrentSession();
		assertSame( s, s2 );
		DummyTransactionManager.INSTANCE.getTransaction().commit();
		assertFalse( s.isOpen() );

		// TODO : would be nice to automate-test that the SF internal map actually gets cleaned up
		//      i verified that is does currently in my debugger...
	}

	public void testCurrentSessionWithIterate() throws Exception {
		DummyTransactionManager.INSTANCE.begin();
		Session s = openSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.persist( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.persist( "Item", item2 );
		DummyTransactionManager.INSTANCE.getTransaction().commit();

		// First, test iterating the partial iterator; iterate to past
		// the first, but not the second, item
		DummyTransactionManager.INSTANCE.begin();
		s = getSessions().getCurrentSession();
		Iterator itr = s.createQuery( "from Item" ).iterate();
		if ( !itr.hasNext() ) {
			fail( "No results in iterator" );
		}
		itr.next();
		if ( !itr.hasNext() ) {
			fail( "Only one result in iterator" );
		}
		DummyTransactionManager.INSTANCE.getTransaction().commit();

		// Next, iterate the entire result
		DummyTransactionManager.INSTANCE.begin();
		s = getSessions().getCurrentSession();
		itr = s.createQuery( "from Item" ).iterate();
		if ( !itr.hasNext() ) {
			fail( "No results in iterator" );
		}
		while ( itr.hasNext() ) {
			itr.next();
		}
		DummyTransactionManager.INSTANCE.getTransaction().commit();

		DummyTransactionManager.INSTANCE.begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		DummyTransactionManager.INSTANCE.getTransaction().commit();
	}

	public void testCurrentSessionWithScroll() throws Exception {
		DummyTransactionManager.INSTANCE.begin();
		Session s = getSessions().getCurrentSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.persist( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.persist( "Item", item2 );
		DummyTransactionManager.INSTANCE.getTransaction().commit();

		// First, test partially scrolling the result with out closing
		DummyTransactionManager.INSTANCE.begin();
		s = getSessions().getCurrentSession();
		ScrollableResults results = s.createQuery( "from Item" ).scroll();
		results.next();
		DummyTransactionManager.INSTANCE.getTransaction().commit();

		// Next, test partially scrolling the result with closing
		DummyTransactionManager.INSTANCE.begin();
		s = getSessions().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		results.next();
		results.close();
		DummyTransactionManager.INSTANCE.getTransaction().commit();

		// Next, scroll the entire result (w/o closing)
		DummyTransactionManager.INSTANCE.begin();
		s = getSessions().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		while ( !results.isLast() ) {
			results.next();
		}
		DummyTransactionManager.INSTANCE.getTransaction().commit();

		// Next, scroll the entire result (closing)
		DummyTransactionManager.INSTANCE.begin();
		s = getSessions().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		while ( !results.isLast() ) {
			results.next();
		}
		results.close();
		DummyTransactionManager.INSTANCE.getTransaction().commit();

		DummyTransactionManager.INSTANCE.begin();
		s = getSessions().getCurrentSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		DummyTransactionManager.INSTANCE.getTransaction().commit();
	}

	public void testAggressiveReleaseWithExplicitDisconnectReconnect() throws Exception {
		DummyTransactionManager.INSTANCE.begin();
		Session s = getSessions().getCurrentSession();

		s.createQuery( "from Item" ).list();

		s.disconnect();
		byte[] bytes = SerializationHelper.serialize( s );
		s = ( Session ) SerializationHelper.deserialize( bytes );
		s.reconnect();

		s.createQuery( "from Item" ).list();

		DummyTransactionManager.INSTANCE.getTransaction().commit();
	}

	public void testAggressiveReleaseWithConnectionRetreival() throws Exception {
		DummyTransactionManager.INSTANCE.begin();
		Session s = openSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.save( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.save( "Item", item2 );
		DummyTransactionManager.INSTANCE.getTransaction().commit();

		try {
			DummyTransactionManager.INSTANCE.begin();
			s = getSessions().getCurrentSession();
			s.createQuery( "from Item" ).scroll().next();
			s.connection();
			DummyTransactionManager.INSTANCE.getTransaction().commit();
		}
		finally {
			DummyTransactionManager.INSTANCE.begin();
			s = openSession();
			s.createQuery( "delete from Item" ).executeUpdate();
			DummyTransactionManager.INSTANCE.getTransaction().commit();
		}
	}

}

