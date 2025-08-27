/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.query.Query;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.CacheSettings.USE_QUERY_CACHE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test case for NaturalId annotation
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class NaturalIdTest extends BaseCoreFunctionalTestCase {
	@After
	public void cleanupData() {
		super.cleanupCache();
		Session s = sessionFactory().openSession();
		s.beginTransaction();
		s.createQuery( "delete NaturalIdOnManyToOne" ).executeUpdate();
		s.createQuery( "delete Citizen" ).executeUpdate();
		s.createQuery( "delete State" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMappingProperties() {
		final EntityMappingType citizenEntityMapping = sessionFactory()
				.getRuntimeMetamodels()
				.getEntityMappingType( Citizen.class );

		final NaturalIdMapping naturalIdMapping = citizenEntityMapping.getNaturalIdMapping();

		assertThat( naturalIdMapping )
				.withFailMessage( "Class should have a natural key" )
				.isNotNull();

		assertThat( naturalIdMapping.getNaturalIdAttributes() )
				.withFailMessage( "Expecting 2 natural-id attributes, got " + naturalIdMapping.getNaturalIdAttributes().size() )
				.hasSize( 2 );
	}

	@Test
	public void testNaturalIdCached() {
		saveSomeCitizens();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		State france = this.getState( s, "Ile de France" );
		NaturalIdLoadAccess<Citizen> loadAccess = s.byNaturalId( Citizen.class )
				.using( "ssn", "1234" )
				.using("state", france );

		this.cleanupCache();

		Statistics stats = sessionFactory().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();
		assertEquals( "Cache hits should be empty", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "Cache puts should be empty", 0, stats.getNaturalIdCachePutCount() );

		// first query
		assertNotNull( loadAccess.load() );
		assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "NaturalId Cache Misses", 1, stats.getNaturalIdCacheMissCount() );
		assertEquals( "NaturalId Cache Puts", 1, stats.getNaturalIdCachePutCount() );
		assertEquals( "NaturalId Cache Queries", 1, stats.getNaturalIdQueryExecutionCount() );

		// query a second time - result should be cached in session
		loadAccess.load();
		assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "NaturalId Cache Misses", 1, stats.getNaturalIdCacheMissCount() );
		assertEquals( "NaturalId Cache Puts", 1, stats.getNaturalIdCachePutCount() );
		assertEquals( "NaturalId Cache Queries", 1, stats.getNaturalIdQueryExecutionCount() );

		// cleanup
		tx.rollback();
		s.close();
	}

	@Test
	public void testNaturalIdLoaderNotCached() {
		saveSomeCitizens();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		State france = this.getState( s, "Ile de France" );
		final NaturalIdLoadAccess<Citizen> naturalIdLoader = s.byNaturalId( Citizen.class );
		naturalIdLoader.using( "ssn", "1234" ).using( "state", france );

		//NaturalId cache gets populated during entity loading, need to clear it out
		this.cleanupCache();
		Statistics stats = sessionFactory().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();
		assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount() );
		assertEquals( "NaturalId Cache Puts", 0, stats.getNaturalIdCachePutCount() );
		assertEquals( "NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount() );

		// first query
		Citizen citizen = naturalIdLoader.load();
		assertNotNull( citizen );
		assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "NaturalId Cache Misses", 1, stats.getNaturalIdCacheMissCount() );
		assertEquals( "NaturalId Cache Puts", 1, stats.getNaturalIdCachePutCount() );
		assertEquals( "NaturalId Cache Queries", 1, stats.getNaturalIdQueryExecutionCount() );

		// cleanup
		tx.rollback();
		s.close();
	}

	@Test
	public void testManyToOneNaturalLoadByNaturalId() {
		NaturalIdOnManyToOne singleManyToOne1 = new NaturalIdOnManyToOne();
		NaturalIdOnManyToOne singleManyToOne2 = new NaturalIdOnManyToOne();

		Citizen c1 = new Citizen();
		c1.setFirstname( "Emmanuel" );
		c1.setLastname( "Bernard" );
		c1.setSsn( "1234" );

		State france = new State();
		france.setName( "Ile de France" );
		c1.setState( france );

		singleManyToOne1.setCitizen( c1 );
		singleManyToOne2.setCitizen( null );

		inTransaction(
				session -> {
					session.persist( france );
					session.persist( c1 );
					session.persist( singleManyToOne1 );
					session.persist( singleManyToOne2 );
				}
		);

		inSession(
				session -> {
					session.getSessionFactory().getCache().evictNaturalIdData(); // we want to go to the database
					session.beginTransaction();
					try {
						NaturalIdOnManyToOne instance1 = session.byNaturalId( NaturalIdOnManyToOne.class )
								.using( "citizen", c1 )
								.load();
						Assertions.assertNotNull( instance1 );
						Assertions.assertNotNull( instance1.getCitizen() );

						NaturalIdOnManyToOne instance2 = session.byNaturalId( NaturalIdOnManyToOne.class )
								.using( "citizen", null ).load();

						Assertions.assertNotNull( instance2 );
						assertNull( instance2.getCitizen() );
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	public void testNaturalIdLoaderCached() {
		Statistics stats = sessionFactory().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();

		assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount() );
		assertEquals( "NaturalId Cache Puts", 0, stats.getNaturalIdCachePutCount() );
		assertEquals( "NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount() );

		saveSomeCitizens();

		assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount() );
		assertEquals( "NaturalId Cache Puts", 2, stats.getNaturalIdCachePutCount() );
		assertEquals( "NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount() );


		//Try NaturalIdLoadAccess after insert

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		State france = this.getState( s, "Ile de France" );
		NaturalIdLoadAccess naturalIdLoader = s.byNaturalId( Citizen.class );
		naturalIdLoader.using( "ssn", "1234" ).using( "state", france );

		//Not clearing naturalId caches, should be warm from entity loading
		stats.clear();

		// first query
		Citizen citizen = (Citizen)naturalIdLoader.load();
		assertNotNull( citizen );
		assertEquals( "NaturalId Cache Hits", 1, stats.getNaturalIdCacheHitCount() );
		assertEquals( "NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount() );
		assertEquals( "NaturalId Cache Puts", 0, stats.getNaturalIdCachePutCount() );
		assertEquals( "NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount() );

		// cleanup
		tx.rollback();
		s.close();


		//Try NaturalIdLoadAccess

		s = openSession();
		tx = s.beginTransaction();

		this.cleanupCache();
		stats.setStatisticsEnabled( true );
		stats.clear();

		// first query
		citizen = (Citizen) s.get( Citizen.class, citizen.getId() );
		assertNotNull( citizen );
		assertEquals( "NaturalId Cache Hits", 0, stats.getNaturalIdCacheHitCount() );
		assertEquals( "NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount() );
		assertEquals( "NaturalId Cache Puts", 1, stats.getNaturalIdCachePutCount() );
		assertEquals( "NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount() );

		// cleanup
		tx.rollback();
		s.close();


		//Try NaturalIdLoadAccess after load

		s = openSession();
		tx = s.beginTransaction();
		france = this.getState( s, "Ile de France" );
		naturalIdLoader = s.byNaturalId( Citizen.class );
		naturalIdLoader.using( "ssn", "1234" ).using( "state", france );

		//Not clearing naturalId caches, should be warm from entity loading
		stats.setStatisticsEnabled( true );
		stats.clear();

		// first query
		citizen = (Citizen)naturalIdLoader.load();
		assertNotNull( citizen );
		assertEquals( "NaturalId Cache Hits", 1, stats.getNaturalIdCacheHitCount() );
		assertEquals( "NaturalId Cache Misses", 0, stats.getNaturalIdCacheMissCount() );
		assertEquals( "NaturalId Cache Puts", 0, stats.getNaturalIdCachePutCount() );
		assertEquals( "NaturalId Cache Queries", 0, stats.getNaturalIdQueryExecutionCount() );

		// cleanup
		tx.rollback();
		s.close();
	}

	@Test
	public void testNaturalIdUncached() {
		saveSomeCitizens();

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		State france = this.getState( s, "Ile de France" );
		final NaturalIdLoadAccess<Citizen> naturalIdLoader = s.byNaturalId( Citizen.class );
		naturalIdLoader.using( "ssn", "1234" ).using( "state", france );

		this.cleanupCache();

		Statistics stats = sessionFactory().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getNaturalIdCacheHitCount()
		);

		// first query
		Citizen citizen = naturalIdLoader.load();
		assertNotNull( citizen );
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getNaturalIdCacheHitCount()
		);
		assertEquals(
				"Query execution count should be one", 1, stats
						.getNaturalIdQueryExecutionCount()
		);

		// query a second time - result should be cached in session
		naturalIdLoader.load();
		assertEquals(
				"Cache hits should be empty", 0, stats
						.getNaturalIdCacheHitCount()
		);
		assertEquals(
				"Second query should not be a miss", 1, stats
						.getNaturalIdCacheMissCount()
		);
		assertEquals(
				"Query execution count should be one", 1, stats
						.getNaturalIdQueryExecutionCount()
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

	private State getState(Session s, String name) {
		final CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<State> criteria = criteriaBuilder.createQuery( State.class );
		Root<State> root = criteria.from( State.class );
		criteria.select( root ).where( criteriaBuilder.equal( root.get( "name" ), name ) );

		Query<State> query = s.createQuery( criteria );
		query.setCacheable( true );
		return query.list().get( 0 );
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( USE_QUERY_CACHE, true );
	}
}
