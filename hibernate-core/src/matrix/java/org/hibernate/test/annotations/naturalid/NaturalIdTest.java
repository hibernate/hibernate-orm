/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.naturalid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test case for NaturalId annotation
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class NaturalIdTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testMappingProperties() {
		ClassMetadata metaData = sessionFactory().getClassMetadata(
				Citizen.class
		);
		assertTrue(
				"Class should have a natural key", metaData
						.hasNaturalIdentifier()
		);
		int[] propertiesIndex = metaData.getNaturalIdentifierProperties();
		assertTrue( "Wrong number of elements", propertiesIndex.length == 2 );
	}

	@Test
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

		Statistics stats = sessionFactory().getStatistics();
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

	@Test
	public void testNaturalIdLoaderCached() {
		saveSomeCitizens();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		State france = ( State ) s.load( State.class, 2 );
		final NaturalIdLoadAccess naturalIdLoader = s.byNaturalId( Citizen.class );
		naturalIdLoader.using( "ssn", "1234" ).using( "state", france );

		Statistics stats = sessionFactory().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getQueryCacheHitCount()
		);

		// first query
		Citizen citizen = (Citizen)naturalIdLoader.load();
		assertNotNull( citizen );
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getNaturalIdCacheHitCount()
		);
		assertEquals(
				"First load should be a miss", 1, stats
						.getNaturalIdCacheMissCount()
		);
		assertEquals(
				"Query result should be added to cache", 1, stats
						.getNaturalIdCachePutCount()
		);

		// query a second time - result should be cached
		citizen = (Citizen)naturalIdLoader.load();
		assertNotNull( citizen );
		assertEquals(
				"Cache hits should be empty", 1, stats
						.getNaturalIdCacheHitCount()
		);

		// cleanup
		tx.rollback();
		s.close();
	}

	@Test
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

		Statistics stats = sessionFactory().getStatistics();
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

	@Override
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

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( "hibernate.cache.use_query_cache", "true" );
	}
}
