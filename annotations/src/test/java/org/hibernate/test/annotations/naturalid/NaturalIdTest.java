//$Id$
package org.hibernate.test.annotations.naturalid;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.stat.Statistics;
import org.hibernate.test.annotations.TestCase;

/**
 * Test case for NaturalId annotation
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class NaturalIdTest extends TestCase {

	public void testMappingProperties() {
		ClassMetadata metaData = getSessions().getClassMetadata(
				Citizen.class
		);
		assertTrue(
				"Class should have a natural key", metaData
						.hasNaturalIdentifier()
		);
		int[] propertiesIndex = metaData.getNaturalIdentifierProperties();
		assertTrue( "Wrong number of elements", propertiesIndex.length == 2 );
	}

	public void testNaturalIdCached() {
		saveSomeCitizens();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		State france = ( State ) s.load( State.class, 2 );
		Criteria criteria = s.createCriteria( Citizen.class );
		criteria.add(
				Restrictions.naturalId().set( "ssn", "1234" ).set(
						"state",
						france
				)
		);
		criteria.setCacheable( true );

		Statistics stats = getSessions().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getQueryCacheHitCount()
		);

		// first query
		List results = criteria.list();
		assertEquals( 1, results.size() );
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getQueryCacheHitCount()
		);
		assertEquals(
				"First query should be a miss", 1, stats
						.getQueryCacheMissCount()
		);
		assertEquals(
				"Query result should be added to cache", 1, stats
						.getQueryCachePutCount()
		);

		// query a second time - result should be cached
		criteria.list();
		assertEquals(
				"Cache hits should be empty", 1, stats
						.getQueryCacheHitCount()
		);

		// cleanup
		tx.rollback();
		s.close();
	}

	public void testNaturalIdUncached() {

		saveSomeCitizens();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		State france = ( State ) s.load( State.class, 2 );
		Criteria criteria = s.createCriteria( Citizen.class );
		criteria.add(
				Restrictions.naturalId().set( "ssn", "1234" ).set(
						"state",
						france
				)
		);
		criteria.setCacheable( false );

		Statistics stats = getSessions().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getQueryCacheHitCount()
		);

		// first query
		List results = criteria.list();
		assertEquals( 1, results.size() );
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getQueryCacheHitCount()
		);
		assertEquals(
				"Query result should be added to cache", 0, stats
						.getQueryCachePutCount()
		);

		// query a second time
		criteria.list();
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getQueryCacheHitCount()
		);

		// cleanup
		tx.rollback();
		s.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Citizen.class, State.class,
				NaturalIdOnManyToOne.class
		};
	}

	private void saveSomeCitizens() {
		Citizen c1 = new Citizen();
		c1.setFirstname( "Emmanuel" );
		c1.setLastname( "Bernard" );
		c1.setSsn( "1234" );

		State france = new State();
		france.setName( "Ile de France" );
		c1.setState( france );

		Citizen c2 = new Citizen();
		c2.setFirstname( "Gavin" );
		c2.setLastname( "King" );
		c2.setSsn( "000" );
		State australia = new State();
		australia.setName( "Australia" );
		c2.setState( australia );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( australia );
		s.persist( france );
		s.persist( c1 );
		s.persist( c2 );
		tx.commit();
		s.close();
	}

	protected void configure(Configuration cfg) {
		cfg.setProperty( "hibernate.cache.use_query_cache", "true" );
	}
}
