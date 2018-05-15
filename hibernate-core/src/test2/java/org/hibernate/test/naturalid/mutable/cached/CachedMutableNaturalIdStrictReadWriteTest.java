/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.mutable.cached;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.NaturalIdCacheStatistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CachedMutableNaturalIdStrictReadWriteTest extends
		CachedMutableNaturalIdTest {

	@Override
	public void configure(Configuration cfg) {
		super.configure(cfg);
		cfg.setProperty( CachingRegionFactory.DEFAULT_ACCESSTYPE, "read-write" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9203" )
	public void testToMapConversion() {
		sessionFactory().getStatistics().clear();

		final Session session = openSession();
		session.getTransaction().begin();
		final AllCached it = new AllCached( "IT" );
		session.save( it );
		session.getTransaction().commit();
		session.close();

		final NaturalIdCacheStatistics stats = sessionFactory().getStatistics().getNaturalIdCacheStatistics(
				"hibernate.test." + AllCached.class.getName() + "##NaturalId"
		);

		assertEquals( 1, stats.getPutCount() );
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-7278" )
	public void testInsertedNaturalIdCachedAfterTransactionSuccess() {
		
		Session session = openSession();
		session.getSessionFactory().getStatistics().clear();
		session.beginTransaction();
		Another it = new Another( "it");
		session.save( it );
		session.flush();
		session.getTransaction().commit();
		session.close();
		
		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId(Another.class).load("it");
		assertNotNull(it);
		session.delete(it);
		session.getTransaction().commit();
		assertEquals(1, session.getSessionFactory().getStatistics().getNaturalIdCacheHitCount());
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-7278" )
	public void testInsertedNaturalIdNotCachedAfterTransactionFailure() {
		
		Session session = openSession();
		session.getSessionFactory().getStatistics().clear();
		session.beginTransaction();
		Another it = new Another( "it");
		session.save( it );
		session.flush();
		session.getTransaction().rollback();
		session.close();
		
		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId(Another.class).load("it");
		assertNull(it);
		assertEquals(0, session.getSessionFactory().getStatistics().getNaturalIdCacheHitCount());
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-7278" )
	public void testChangedNaturalIdCachedAfterTransactionSuccess() {
		Session session = openSession();
		session.beginTransaction();
		Another it = new Another( "it");
		session.save( it );
		session.getTransaction().commit();
		session.close();
		
		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId(Another.class).load("it");
		assertNotNull(it);
		
		it.setName("modified");
		session.flush();
		session.getTransaction().commit(); 
		session.close();
		
		session.getSessionFactory().getStatistics().clear();
		
		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId(Another.class).load("modified");
		assertNotNull(it);
		session.delete(it);
		session.getTransaction().commit(); 
		session.close();
		
		assertEquals(1, session.getSessionFactory().getStatistics().getNaturalIdCacheHitCount());
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-7278" )
	public void testChangedNaturalIdNotCachedAfterTransactionFailure() {
		Session session = openSession();
		session.beginTransaction();
		Another it = new Another( "it");
		session.save( it );
		session.getTransaction().commit();
		session.close();
		
		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId(Another.class).load("it");
		assertNotNull(it);
		
		it.setName("modified");
		session.flush();
		session.getTransaction().rollback(); 
		session.close();
		
		session.getSessionFactory().getStatistics().clear();
		
		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId(Another.class).load("modified");
		assertNull(it);
		it = (Another) session.bySimpleNaturalId(Another.class).load("it");
		session.delete(it);
		session.getTransaction().commit(); 
		session.close();
		
		assertEquals(0, session.getSessionFactory().getStatistics().getNaturalIdCacheHitCount());
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-7309" )
	public void testInsertUpdateEntity_NaturalIdCachedAfterTransactionSuccess() {
		
		Session session = openSession();
		session.getSessionFactory().getStatistics().clear();
		session.beginTransaction();
		Another it = new Another( "it");
		session.save( it );    // schedules an InsertAction
		it.setSurname("1234"); // schedules an UpdateAction, without bug-fix
		// this will re-cache natural-id with identical key and at same time invalidate it
		session.flush();
		session.getTransaction().commit();
		session.close();
		
		session = openSession();
		session.beginTransaction();
		it = (Another) session.bySimpleNaturalId(Another.class).load("it");
		assertNotNull(it);
		session.delete(it);
		session.getTransaction().commit();
		assertEquals("In a strict access strategy we would excpect a hit here", 1, session.getSessionFactory().getStatistics().getNaturalIdCacheHitCount());
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9200" )
	public void testNaturalIdCacheStatisticsReset() {
		final String naturalIdCacheRegion = "hibernate.test.org.hibernate.test.naturalid.mutable.cached.Another##NaturalId";
		sessionFactory().getStatistics().clear();

		Session session = openSession();
		session.beginTransaction();
		final Another it = new Another( "IT");
		session.save( it );
		session.getTransaction().commit();
		session.close();

		NaturalIdCacheStatistics statistics = sessionFactory().getStatistics().getNaturalIdCacheStatistics( naturalIdCacheRegion );
		assertEquals( 1, statistics.getPutCount() );

		sessionFactory().getStatistics().clear();

		// Refresh statistics reference.
		statistics = sessionFactory().getStatistics().getNaturalIdCacheStatistics( naturalIdCacheRegion );
		assertEquals( 0, statistics.getPutCount() );

		session = openSession();
		session.beginTransaction();
		session.delete( it );
		session.getTransaction().commit();
		session.clear();
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
}
