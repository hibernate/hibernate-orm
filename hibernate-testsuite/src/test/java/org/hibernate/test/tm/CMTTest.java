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
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.testing.tm.ConnectionProviderImpl;
import org.hibernate.testing.tm.SimpleJtaTransactionManagerImpl;
import org.hibernate.testing.tm.TransactionManagerLookupImpl;
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
		cfg.setProperty( Environment.CONNECTION_PROVIDER, ConnectionProviderImpl.class.getName() );
		cfg.setProperty( Environment.TRANSACTION_MANAGER_STRATEGY, TransactionManagerLookupImpl.class.getName() );
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

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		getSessions().evictEntity( "Item" );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s1 = openSession();
		foo = ( Map ) s1.get( "Item", "Foo" );
		//foo.put("description", "a big red foo");
		//s1.flush();
		Transaction tx1 = SimpleJtaTransactionManagerImpl.getInstance().suspend();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s2 = openSession();
		foo = ( Map ) s2.get( "Item", "Foo" );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().resume( tx1 );
		tx1.commit();

		getSessions().evictEntity( "Item" );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s1 = openSession();
		s1.createCriteria( "Item" ).list();
		//foo.put("description", "a big red foo");
		//s1.flush();
		tx1 = SimpleJtaTransactionManagerImpl.getInstance().suspend();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s2 = openSession();
		s2.createCriteria( "Item" ).list();
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		SimpleJtaTransactionManagerImpl.getInstance().resume( tx1 );
		tx1.commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s2 = openSession();
		s2.createCriteria( "Item" ).list();
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertEquals( 7, getSessions().getStatistics().getEntityLoadCount() );
		assertEquals( 0, getSessions().getStatistics().getEntityFetchCount() );
		assertEquals( 3, getSessions().getStatistics().getQueryExecutionCount() );
		assertEquals( 0, getSessions().getStatistics().getQueryCacheHitCount() );
		assertEquals( 0, getSessions().getStatistics().getQueryCacheMissCount() );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		SimpleJtaTransactionManagerImpl.getInstance().commit();
	}

	public void testConcurrentCachedQueries() throws Exception {

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		synchronized ( this ) {
			wait( 1000 );
		}

		getSessions().getStatistics().clear();

		getSessions().evictEntity( "Item" );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s4 = openSession();
		Transaction tx4 = SimpleJtaTransactionManagerImpl.getInstance().suspend();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s1 = openSession();
		List r1 = s1.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r1.size(), 2 );
		Transaction tx1 = SimpleJtaTransactionManagerImpl.getInstance().suspend();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s2 = openSession();
		List r2 = s2.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r2.size(), 2 );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 2 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 1 );

		SimpleJtaTransactionManagerImpl.getInstance().resume( tx1 );
		tx1.commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s3 = openSession();
		s3.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 4 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 2 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 2 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 1 );

		SimpleJtaTransactionManagerImpl.getInstance().resume( tx4 );
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

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		SimpleJtaTransactionManagerImpl.getInstance().commit();
	}

	public void testConcurrentCachedDirtyQueries() throws Exception {
		if ( getDialect().doesReadCommittedCauseWritersToBlockReaders() ) {
			reportSkip( "write locks block readers", "concurrent queries" );
			return;
		}

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Map foo = new HashMap();
		foo.put( "name", "Foo" );
		foo.put( "description", "a big foo" );
		s.persist( "Item", foo );
		Map bar = new HashMap();
		bar.put( "name", "Bar" );
		bar.put( "description", "a small bar" );
		s.persist( "Item", bar );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		synchronized ( this ) {
			wait( 1000 );
		}

		getSessions().getStatistics().clear();

		getSessions().evictEntity( "Item" );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s4 = openSession();
		Transaction tx4 = SimpleJtaTransactionManagerImpl.getInstance().suspend();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s1 = openSession();
		List r1 = s1.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r1.size(), 2 );
		foo = ( Map ) r1.get( 0 );
		foo.put( "description", "a big red foo" );
		s1.flush();
		Transaction tx1 = SimpleJtaTransactionManagerImpl.getInstance().suspend();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s2 = openSession();
		List r2 = s2.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		assertEquals( r2.size(), 2 );
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 4 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 2 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 2 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 2 );

		SimpleJtaTransactionManagerImpl.getInstance().resume( tx1 );
		tx1.commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s3 = openSession();
		s3.createCriteria( "Item" ).addOrder( Order.asc( "description" ) )
				.setCacheable( true ).list();
		SimpleJtaTransactionManagerImpl.getInstance().commit();

		assertEquals( getSessions().getStatistics().getSecondLevelCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getSecondLevelCacheMissCount(), 0 );
		assertEquals( getSessions().getStatistics().getEntityLoadCount(), 6 );
		assertEquals( getSessions().getStatistics().getEntityFetchCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 3 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 3 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheMissCount(), 3 );

		SimpleJtaTransactionManagerImpl.getInstance().resume( tx4 );
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

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		SimpleJtaTransactionManagerImpl.getInstance().commit();
	}

	public void testCMT() throws Exception {
		getSessions().getStatistics().clear();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();
		assertFalse( s.isOpen() );

		assertEquals( getSessions().getStatistics().getFlushCount(), 0 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().rollback();
		assertFalse( s.isOpen() );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		Map item = new HashMap();
		item.put( "name", "The Item" );
		item.put( "description", "The only item we have" );
		s.persist( "Item", item );
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();
		assertFalse( s.isOpen() );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		item = ( Map ) s.createQuery( "from Item" ).uniqueResult();
		assertNotNull( item );
		s.delete( item );
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();
		assertFalse( s.isOpen() );

		assertEquals( getSessions().getStatistics().getTransactionCount(), 4 );
		assertEquals( getSessions().getStatistics().getSuccessfulTransactionCount(), 3 );
		assertEquals( getSessions().getStatistics().getEntityDeleteCount(), 1 );
		assertEquals( getSessions().getStatistics().getEntityInsertCount(), 1 );
		assertEquals( getSessions().getStatistics().getSessionOpenCount(), 4 );
		assertEquals( getSessions().getStatistics().getSessionCloseCount(), 4 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getFlushCount(), 2 );

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		SimpleJtaTransactionManagerImpl.getInstance().commit();

	}

	public void testCurrentSession() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = getSessions().getCurrentSession();
		Session s2 = getSessions().getCurrentSession();
		assertSame( s, s2 );
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();
		assertFalse( s.isOpen() );

		// TODO : would be nice to automate-test that the SF internal map actually gets cleaned up
		//      i verified that is does currently in my debugger...
	}

	public void testCurrentSessionWithIterate() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.persist( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.persist( "Item", item2 );
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();

		// First, test iterating the partial iterator; iterate to past
		// the first, but not the second, item
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = getSessions().getCurrentSession();
		Iterator itr = s.createQuery( "from Item" ).iterate();
		if ( !itr.hasNext() ) {
			fail( "No results in iterator" );
		}
		itr.next();
		if ( !itr.hasNext() ) {
			fail( "Only one result in iterator" );
		}
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();

		// Next, iterate the entire result
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = getSessions().getCurrentSession();
		itr = s.createQuery( "from Item" ).iterate();
		if ( !itr.hasNext() ) {
			fail( "No results in iterator" );
		}
		while ( itr.hasNext() ) {
			itr.next();
		}
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = openSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();
	}

	public void testCurrentSessionWithScroll() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = getSessions().getCurrentSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.persist( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.persist( "Item", item2 );
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();

		// First, test partially scrolling the result with out closing
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = getSessions().getCurrentSession();
		ScrollableResults results = s.createQuery( "from Item" ).scroll();
		results.next();
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();

		// Next, test partially scrolling the result with closing
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = getSessions().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		results.next();
		results.close();
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();

		// Next, scroll the entire result (w/o closing)
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = getSessions().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		while ( results.next() ) {
			// do nothing
		}
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();

		// Next, scroll the entire result (closing)
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = getSessions().getCurrentSession();
		results = s.createQuery( "from Item" ).scroll();
		while ( results.next() ) {
			// do nothing
		}
		results.close();
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();

		SimpleJtaTransactionManagerImpl.getInstance().begin();
		s = getSessions().getCurrentSession();
		s.createQuery( "delete from Item" ).executeUpdate();
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();
	}

	public void testAggressiveReleaseWithExplicitDisconnectReconnect() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = getSessions().getCurrentSession();

		s.createQuery( "from Item" ).list();

		s.disconnect();
		byte[] bytes = SerializationHelper.serialize( s );
		s = ( Session ) SerializationHelper.deserialize( bytes );
		s.reconnect();

		s.createQuery( "from Item" ).list();

		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();
	}

	public void testAggressiveReleaseWithConnectionRetreival() throws Exception {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
		Session s = openSession();
		Map item1 = new HashMap();
		item1.put( "name", "Item - 1" );
		item1.put( "description", "The first item" );
		s.save( "Item", item1 );

		Map item2 = new HashMap();
		item2.put( "name", "Item - 2" );
		item2.put( "description", "The second item" );
		s.save( "Item", item2 );
		SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();

		try {
			SimpleJtaTransactionManagerImpl.getInstance().begin();
			s = getSessions().getCurrentSession();
			s.createQuery( "from Item" ).scroll().next();
			s.connection();
			SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();
		}
		finally {
			SimpleJtaTransactionManagerImpl.getInstance().begin();
			s = openSession();
			s.createQuery( "delete from Item" ).executeUpdate();
			SimpleJtaTransactionManagerImpl.getInstance().getTransaction().commit();
		}
	}

}

