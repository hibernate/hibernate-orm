/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.CacheSettings.USE_QUERY_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test case for NaturalId annotation
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name = USE_QUERY_CACHE, value = "true"))
@DomainModel( annotatedClasses = { Citizen.class, State.class, NaturalIdOnManyToOne.class } )
@SessionFactory
public class NaturalIdTest {
	@AfterEach
	public void cleanupData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
		cleanupCaches( factoryScope );
	}

	protected void cleanupCaches(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory().getCache().evictAllRegions();
	}

	@Test
	public void testMappingProperties(SessionFactoryScope factoryScope) {
		final EntityMappingType citizenEntityMapping = factoryScope
				.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Citizen.class );

		final NaturalIdMapping naturalIdMapping = citizenEntityMapping.getNaturalIdMapping();

		assertThat( naturalIdMapping )
				.withFailMessage( "Class should have a natural key" )
				.isNotNull();

		assertThat( naturalIdMapping.getNaturalIdAttributes() )
				.withFailMessage( "Expecting 2 natural-id attributes, got " + naturalIdMapping.getNaturalIdAttributes().size() )
				.hasSize( 2 );
	}

	@Test
	public void testNaturalIdCached(SessionFactoryScope factoryScope) {
		saveSomeCitizens( factoryScope );

		final var sessionFactory = factoryScope.getSessionFactory();
		final var stats = sessionFactory.getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();

		factoryScope.inTransaction( (session) -> {
			var france = session.find( State.class, 1 );
			var loadAccess = session.byNaturalId( Citizen.class )
					.using( "ssn", "1234" )
					.using("state", france );

			cleanupCaches( factoryScope );

			assertEquals( 0, stats.getNaturalIdCacheHitCount(), "Cache hits should be empty" );
			assertEquals( 0, stats.getNaturalIdCachePutCount(), "Cache puts should be empty" );

			// first query
			assertNotNull( loadAccess.load() );
			assertEquals( 0, stats.getNaturalIdCacheHitCount(), "NaturalId Cache Hits" );
			assertEquals( 1, stats.getNaturalIdCacheMissCount(), "NaturalId Cache Misses" );
			assertEquals( 1, stats.getNaturalIdCachePutCount(), "NaturalId Cache Puts" );
			assertEquals( 1, stats.getNaturalIdQueryExecutionCount(), "NaturalId Cache Queries" );

			// query a second time - result should be cached in session
			loadAccess.load();
			assertEquals( 0, stats.getNaturalIdCacheHitCount(), "NaturalId Cache Hits" );
			assertEquals( 1, stats.getNaturalIdCacheMissCount(), "NaturalId Cache Misses" );
			assertEquals( 1, stats.getNaturalIdCachePutCount(), "NaturalId Cache Puts" );
			assertEquals( 1, stats.getNaturalIdQueryExecutionCount(), "NaturalId Cache Queries" );
		} );
	}

	@Test
	public void testNaturalIdLoaderNotCached(SessionFactoryScope factoryScope) {
		saveSomeCitizens( factoryScope );

		final var sessionFactory = factoryScope.getSessionFactory();
		final var stats = sessionFactory.getStatistics();
		stats.setStatisticsEnabled( true );

		factoryScope.inTransaction(  (session) -> {
			var france = session.find( State.class, 1 );
			var naturalIdLoader = session.byNaturalId( Citizen.class );
			naturalIdLoader.using( "ssn", "1234" ).using( "state", france );

			//NaturalId cache gets populated during entity loading, need to clear it out
			cleanupCaches( factoryScope );
			stats.clear();

			assertEquals( 0, stats.getNaturalIdCacheHitCount(), "NaturalId Cache Hits" );
			assertEquals( 0, stats.getNaturalIdCacheMissCount(), "NaturalId Cache Misses" );
			assertEquals( 0, stats.getNaturalIdCachePutCount(), "NaturalId Cache Puts" );
			assertEquals( 0, stats.getNaturalIdQueryExecutionCount(), "NaturalId Cache Queries" );

			// first query
			assertNotNull( naturalIdLoader.load() );
			assertEquals( 0, stats.getNaturalIdCacheHitCount(), "NaturalId Cache Hits" );
			assertEquals( 1, stats.getNaturalIdCacheMissCount(), "NaturalId Cache Misses" );
			assertEquals( 1, stats.getNaturalIdCachePutCount(), "NaturalId Cache Puts" );
			assertEquals( 1, stats.getNaturalIdQueryExecutionCount(), "NaturalId Cache Queries" );

		} );
	}

	@Test
	public void testManyToOneNaturalLoadByNaturalId(SessionFactoryScope factoryScope) {
		saveSomeCitizens( factoryScope );

		var citizen1 = factoryScope.fromTransaction( (session) -> {
			NaturalIdOnManyToOne singleManyToOne1 = new NaturalIdOnManyToOne();
			NaturalIdOnManyToOne singleManyToOne2 = new NaturalIdOnManyToOne();

			final Citizen citizen = session.find( Citizen.class, 1 );
			singleManyToOne1.setCitizen( citizen );
			singleManyToOne2.setCitizen( null );

			session.persist( singleManyToOne1 );
			session.persist( singleManyToOne2 );

			return citizen;
		} );

		factoryScope.inTransaction( (session) -> {
			// we want to go to the database
			session.getSessionFactory().getCache().evictNaturalIdData();

			var instance1 = session.byNaturalId( NaturalIdOnManyToOne.class )
					.using( "citizen", citizen1 )
					.load();
			assertNotNull( instance1 );
			assertNotNull( instance1.getCitizen() );

			var instance2 = session.byNaturalId( NaturalIdOnManyToOne.class )
					.using( "citizen", null )
					.load();
			assertNotNull( instance2 );
			assertNull( instance2.getCitizen() );
		} );
	}

	@Test
	public void testNaturalIdLoaderCached(SessionFactoryScope factoryScope) {
		final var stats = factoryScope.getSessionFactory().getStatistics();
		stats.setStatisticsEnabled( true );
		stats.clear();

		assertEquals( 0, stats.getNaturalIdCacheHitCount(), "NaturalId Cache Hits" );
		assertEquals( 0, stats.getNaturalIdCacheMissCount(), "NaturalId Cache Misses" );
		assertEquals( 0, stats.getNaturalIdCachePutCount(), "NaturalId Cache Puts" );
		assertEquals( 0, stats.getNaturalIdQueryExecutionCount(), "NaturalId Cache Queries" );

		saveSomeCitizens( factoryScope );

		assertEquals( 0, stats.getNaturalIdCacheHitCount(), "NaturalId Cache Hits" );
		assertEquals( 0, stats.getNaturalIdCacheMissCount(), "NaturalId Cache Misses" );
		assertEquals( 2, stats.getNaturalIdCachePutCount(), "NaturalId Cache Puts" );
		assertEquals( 0, stats.getNaturalIdQueryExecutionCount(), "NaturalId Cache Queries" );


		//Try NaturalIdLoadAccess after insert
		factoryScope.inTransaction( (session) -> {
			var france = session.find( State.class, 1 );
			var naturalIdLoader = session
					.byNaturalId( Citizen.class )
					.using( "ssn", "1234" )
					.using( "state", france );

			//Not clearing naturalId caches, should be warm from entity loading
			stats.clear();

			// first query
			assertNotNull( naturalIdLoader.load() );
			assertEquals( 1, stats.getNaturalIdCacheHitCount(), "NaturalId Cache Hits" );
			assertEquals( 0, stats.getNaturalIdCacheMissCount(), "NaturalId Cache Misses" );
			assertEquals( 0, stats.getNaturalIdCachePutCount(), "NaturalId Cache Puts" );
			assertEquals( 0, stats.getNaturalIdQueryExecutionCount(), "NaturalId Cache Queries" );
		} );


		//Try NaturalIdLoadAccess
		factoryScope.inTransaction( (session) -> {
			cleanupCaches( factoryScope );
			stats.clear();

			// first query
			assertThat( session.find( Citizen.class, 1 ) ).isNotNull();
			assertEquals( 0, stats.getNaturalIdCacheHitCount(), "NaturalId Cache Hits" );
			assertEquals( 0, stats.getNaturalIdCacheMissCount(), "NaturalId Cache Misses" );
			assertEquals( 1, stats.getNaturalIdCachePutCount(), "NaturalId Cache Puts" );
			assertEquals( 0, stats.getNaturalIdQueryExecutionCount(), "NaturalId Cache Queries" );
		} );


		//Try NaturalIdLoadAccess after load
		factoryScope.inTransaction( (session) -> {
			var france = session.find( State.class, 1 );
			var naturalIdLoader = session.byNaturalId( Citizen.class )
					.using( "ssn", "1234" )
					.using( "state", france );

			//Not clearing naturalId caches, should be warm from entity loading
			stats.clear();

			// first query
			assertThat( naturalIdLoader.load() ).isNotNull();
			assertEquals( 1, stats.getNaturalIdCacheHitCount(), "NaturalId Cache Hits" );
			assertEquals( 0, stats.getNaturalIdCacheMissCount(), "NaturalId Cache Misses" );
			assertEquals( 0, stats.getNaturalIdCachePutCount(), "NaturalId Cache Puts" );
			assertEquals( 0, stats.getNaturalIdQueryExecutionCount(), "NaturalId Cache Queries" );
		} );
	}

	@Test
	public void testNaturalIdUncached(SessionFactoryScope factoryScope) {
		saveSomeCitizens( factoryScope );

		var stats = factoryScope.getSessionFactory().getStatistics();
		stats.setStatisticsEnabled( true );

		factoryScope.inTransaction( (session) -> {
			var france = session.find( State.class, 1 );
			var naturalIdLoader = session.byNaturalId( Citizen.class )
					.using( "ssn", "1234" )
					.using( "state", france );

			cleanupCaches( factoryScope );
			stats.clear();

			assertEquals( 0, stats.getNaturalIdCacheHitCount(), "Cache hits should be empty" );

			// first query
			Citizen citizen = naturalIdLoader.load();
			assertNotNull( citizen );
			assertEquals( 0, stats.getNaturalIdCacheHitCount(), "Cache hits should be empty" );
			assertEquals( 1, stats.getNaturalIdQueryExecutionCount(), "Query execution count should be one" );

			// query a second time - result should be cached in session
			naturalIdLoader.load();
			assertEquals( 0, stats.getNaturalIdCacheHitCount(), "Cache hits should be empty" );
			assertEquals( 1, stats.getNaturalIdCacheMissCount(), "Second query should not be a miss" );
			assertEquals( 1, stats.getNaturalIdQueryExecutionCount(), "Query execution count should be one" );
		} );
	}

	private void saveSomeCitizens(SessionFactoryScope factoryScope) {
		State france = new State( 1, "Ile de France" );
		Citizen c1 = new Citizen( 1, "Emmanuel", "Bernard", france, "1234" );

		State australia = new State( 2, "Australia" );
		Citizen c2 = new Citizen( 2, "Gavin", "King", australia, "000" );

		factoryScope.inTransaction( (session) -> {
			session.persist( australia );
			session.persist( france );
			session.persist( c1 );
			session.persist( c2 );
		} );
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
}
