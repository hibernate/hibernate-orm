/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				SharedDomainDataAndQueryResultsTest.Dog.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true"),
				@Setting(name = Environment.CACHE_REGION_PREFIX, value = SharedDomainDataAndQueryResultsTest.PREFIX),
				@Setting(name = Environment.CACHE_REGION_FACTORY, value = "org.hibernate.testing.cache.CachingRegionFactory"),
		}
)
@SessionFactory(generateStatistics = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SharedDomainDataAndQueryResultsTest {

	private static final String QUERY = "SELECT a FROM Dog a";
	private static final String REGION = "TheRegion";
	static final String PREFIX = "test";

	@Order( 2 )
	@Test
	@JiraKey(value = "HHH-13586")
	public void testAllCachedStatistics(SessionFactoryScope scope) {
		final Statistics statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		final CacheRegionStatistics regionStatistics = statistics.getCacheRegionStatistics( REGION );

		final QueryStatistics queryStatistics = statistics.getQueryStatistics( QUERY );

		scope.inTransaction( session -> {

					Dog yogi = session.get( Dog.class, "Yogi" );

					assertEquals( 1, statistics.getSecondLevelCacheHitCount() );
					assertEquals( 0, statistics.getSecondLevelCachePutCount() );
					assertEquals( 0, statistics.getSecondLevelCacheMissCount() );

					assertEquals( 1, regionStatistics.getHitCount() );
					assertEquals( 0, regionStatistics.getPutCount() );
					assertEquals( 0, regionStatistics.getMissCount() );

					assertEquals( 0, statistics.getQueryCacheHitCount() );
					assertEquals( 0, statistics.getQueryCachePutCount() );
					assertEquals( 0, statistics.getQueryCacheMissCount() );

					assertEquals( 0, queryStatistics.getCacheHitCount() );
					assertEquals( 0, queryStatistics.getCachePutCount() );
					assertEquals( 0, queryStatistics.getCacheMissCount() );

					assertFalse( Hibernate.isInitialized( yogi.nickNames ) );
					Hibernate.initialize( yogi.nickNames );

					assertEquals( 1, statistics.getSecondLevelCacheHitCount() );
					assertEquals( 1, statistics.getSecondLevelCachePutCount() );
					assertEquals( 1, statistics.getSecondLevelCacheMissCount() );

					assertEquals( 1, regionStatistics.getHitCount() );
					assertEquals( 1, regionStatistics.getPutCount() );
					assertEquals( 1, regionStatistics.getMissCount() );

					assertEquals( 0, statistics.getQueryCacheHitCount() );
					assertEquals( 0, statistics.getQueryCachePutCount() );
					assertEquals( 0, statistics.getQueryCacheMissCount() );

					assertEquals( 0, queryStatistics.getCacheHitCount() );
					assertEquals( 0, queryStatistics.getCachePutCount() );
					assertEquals( 0, queryStatistics.getCacheMissCount() );
				}
		);

		scope.inTransaction( session -> {

					Dog yogi = session.get( Dog.class, "Yogi" );

					assertEquals( 2, statistics.getSecondLevelCacheHitCount() );
					assertEquals( 1, statistics.getSecondLevelCachePutCount() );
					assertEquals( 1, statistics.getSecondLevelCacheMissCount() );

					assertEquals( 2, regionStatistics.getHitCount() );
					assertEquals( 1, regionStatistics.getPutCount() );
					assertEquals( 1, regionStatistics.getMissCount() );

					assertEquals( 0, statistics.getQueryCacheHitCount() );
					assertEquals( 0, statistics.getQueryCachePutCount() );
					assertEquals( 0, statistics.getQueryCacheMissCount() );

					assertEquals( 0, queryStatistics.getCacheHitCount() );
					assertEquals( 0, queryStatistics.getCachePutCount() );
					assertEquals( 0, queryStatistics.getCacheMissCount() );

					assertFalse( Hibernate.isInitialized( yogi.nickNames ) );
					Hibernate.initialize( yogi.nickNames );

					assertEquals( 3, statistics.getSecondLevelCacheHitCount() );
					assertEquals( 1, statistics.getSecondLevelCachePutCount() );
					assertEquals( 1, statistics.getSecondLevelCacheMissCount() );

					assertEquals( 3, regionStatistics.getHitCount() );
					assertEquals( 1, regionStatistics.getPutCount() );
					assertEquals( 1, regionStatistics.getMissCount() );

					assertEquals( 0, statistics.getQueryCacheHitCount() );
					assertEquals( 0, statistics.getQueryCachePutCount() );
					assertEquals( 0, statistics.getQueryCacheMissCount() );

					assertEquals( 0, queryStatistics.getCacheHitCount() );
					assertEquals( 0, queryStatistics.getCachePutCount() );
					assertEquals( 0, queryStatistics.getCacheMissCount() );

				}
		);

		scope.inTransaction( session -> {

					List<Dog> dogs = session.createNamedQuery( "Dog.findAll", Dog.class ).list();

					assertEquals( 2, dogs.size() );

					// statistics.getSecondLevelCacheHitCount() only includes entity/collection hits
					assertEquals( 3, statistics.getSecondLevelCacheHitCount() );
					// statistics.getSecondLevelCachePutCount() only includes entity/collection puts
					assertEquals( 3, statistics.getSecondLevelCachePutCount() );
					// statistics.getSecondLevelCacheMissCount() only includes entity/collection misses
					assertEquals( 1, statistics.getSecondLevelCacheMissCount() );

					// regionStatistics has hits/puts/misses for entities/collections/query results
					// Query results missed and put in cache.
					// Reference caching is not being used, so entities are not looked up in the cache.
					// Entities get put in cache.
					assertEquals( 3, regionStatistics.getHitCount() );
					// 2 Dog puts; 1 query put
					assertEquals( 4, regionStatistics.getPutCount() );
					// 1 query miss
					assertEquals( 2, regionStatistics.getMissCount() );

					assertEquals( 0, statistics.getQueryCacheHitCount() );
					assertEquals( 1, statistics.getQueryCachePutCount() );
					assertEquals( 1, statistics.getQueryCacheMissCount() );

					assertEquals( 0, queryStatistics.getCacheHitCount() );
					assertEquals( 1, queryStatistics.getCachePutCount() );
					assertEquals( 1, queryStatistics.getCacheMissCount() );

					for ( Dog dog : dogs ) {
						assertFalse( Hibernate.isInitialized( dog.nickNames ) );
						Hibernate.initialize( dog.nickNames );
					}

					// yogi.nickNames will be found in the cache as a cache hit.
					// The other collection will be a cache miss and will be put in the cache
					// statistics.getSecondLevelCacheHitCount() only includes entity/collection hits
					assertEquals( 4, statistics.getSecondLevelCacheHitCount() );
					// statistics.getSecondLevelCachePutCount() only includes entity/collection puts
					assertEquals( 4, statistics.getSecondLevelCachePutCount() );
					// statistics.getSecondLevelCacheMissCount() only includes entity/collection misses
					assertEquals( 2, statistics.getSecondLevelCacheMissCount() );

					// regionStatistics includes hits/puts/misses for entities/collections/query results
					assertEquals( 4, regionStatistics.getHitCount() );
					// 2 Dog puts; 1 query put
					assertEquals( 5, regionStatistics.getPutCount() );
					// 1 query miss
					assertEquals( 3, regionStatistics.getMissCount() );

					assertEquals( 0, statistics.getQueryCacheHitCount() );
					assertEquals( 1, statistics.getQueryCachePutCount() );
					assertEquals( 1, statistics.getQueryCacheMissCount() );

					assertEquals( 0, queryStatistics.getCacheHitCount() );
					assertEquals( 1, queryStatistics.getCachePutCount() );
					assertEquals( 1, queryStatistics.getCacheMissCount() );

				}
		);

		scope.inTransaction( session -> {

					List<Dog> dogs = session.getNamedQuery( "Dog.findAll" ).list();

					assertEquals( 2, dogs.size() );

					// statistics.getSecondLevelCacheHitCount() only includes entity/collection hits
					assertEquals( 4, statistics.getSecondLevelCacheHitCount() );
					// statistics.getSecondLevelCachePutCount() only includes entity/collection puts
					assertEquals( 4, statistics.getSecondLevelCachePutCount() );
					// statistics.getSecondLevelCacheMissCount() only includes entity/collection misses
					assertEquals( 2, statistics.getSecondLevelCacheMissCount() );

					// regionStatistics includes hits/puts/misses for entities/collections/query results
					// Query results will be found in the cache.
					assertEquals( 5, regionStatistics.getHitCount() );
					assertEquals( 5, regionStatistics.getPutCount() );
					assertEquals( 3, regionStatistics.getMissCount() );

					assertEquals( 1, statistics.getQueryCacheHitCount() );
					assertEquals( 1, statistics.getQueryCachePutCount() );
					assertEquals( 1, statistics.getQueryCacheMissCount() );

					assertEquals( 1, queryStatistics.getCacheHitCount() );
					assertEquals( 1, queryStatistics.getCachePutCount() );
					assertEquals( 1, queryStatistics.getCacheMissCount() );

					for ( Dog dog : dogs ) {
						assertFalse( Hibernate.isInitialized( dog.nickNames ) );
						Hibernate.initialize( dog.nickNames );
					}

					// Both Dog.nickNames will be found in the cache as a cache hit.

					// statistics.getSecondLevelCacheHitCount() only includes entity/collection hits
					assertEquals( 6, statistics.getSecondLevelCacheHitCount() );
					// statistics.getSecondLevelCachePutCount() only includes entity/collection puts
					assertEquals( 4, statistics.getSecondLevelCachePutCount() );
					// statistics.getSecondLevelCacheMissCount() only includes entity/collection misses
					assertEquals( 2, statistics.getSecondLevelCacheMissCount() );

					// regionStatistics includes hits/puts/misses for entities/collections/query results
					assertEquals( 7, regionStatistics.getHitCount() );
					// 2 Dog puts; 1 query put
					assertEquals( 5, regionStatistics.getPutCount() );
					// 1 query miss
					assertEquals( 3, regionStatistics.getMissCount() );

					assertEquals( 1, statistics.getQueryCacheHitCount() );
					assertEquals( 1, statistics.getQueryCachePutCount() );
					assertEquals( 1, statistics.getQueryCacheMissCount() );

					assertEquals( 1, queryStatistics.getCacheHitCount() );
					assertEquals( 1, queryStatistics.getCachePutCount() );
					assertEquals( 1, queryStatistics.getCacheMissCount() );
				}
		);
	}

	@Order( 1 )
	@Test
	@JiraKey(value = "HHH-13586")
	public void testCacheImplementorGetRegion(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();
		final Region domainDataRegion = cache.getRegion( REGION );
		assertTrue( DomainDataRegion.class.isInstance( domainDataRegion ) );
		assertEquals( REGION, domainDataRegion.getName() );

		// There should not be a QueryResultsRegion named REGION until
		// the named query is executed.
		assertNull( cache.getQueryResultsCacheStrictly( REGION ) );

		scope.inTransaction( session -> {
			session.createNamedQuery( "Dog.findAll", Dog.class ).list();
		} );

		// No there should be a QueryResultsCache named REGION
		final QueryResultsCache queryResultsCache = cache.getQueryResultsCacheStrictly( REGION );
		assertNotNull( queryResultsCache );
		assertEquals( REGION, queryResultsCache.getRegion().getName() );

		// Now there is a DomainDataRegion and QueryResultsRegion named REGION.
		// Make sure that the same DomainDataRegion is returned by cache.getRegion( REGION ).
		assertSame( domainDataRegion, cache.getRegion( REGION ) );
	}

	@Order( 2 )
	@Test
	@JiraKey(value = "HHH-13586")
	public void testEvictCaches(SessionFactoryScope scope) {

		final Statistics statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		assertEquals( 0, statistics.getSecondLevelCacheHitCount() );
		assertEquals( 0, statistics.getSecondLevelCachePutCount() );
		assertEquals( 0, statistics.getSecondLevelCacheMissCount() );

		assertEquals( 0, statistics.getQueryCacheHitCount() );
		assertEquals( 0, statistics.getQueryCachePutCount() );
		assertEquals( 0, statistics.getQueryCacheMissCount() );

		scope.inTransaction( session -> {
					Dog yogi = session.get( Dog.class, "Yogi" );
					assertEquals( 1, statistics.getSecondLevelCacheHitCount() );
					assertEquals( 0, statistics.getSecondLevelCachePutCount() );
					assertEquals( 0, statistics.getSecondLevelCacheMissCount() );
					// put the collection in the cache
					Hibernate.initialize( yogi.nickNames );
					assertEquals( 1, statistics.getSecondLevelCacheHitCount() );
					assertEquals( 1, statistics.getSecondLevelCachePutCount() );
					assertEquals( 1, statistics.getSecondLevelCacheMissCount() );

					session.createNamedQuery( "Dog.findAll", Dog.class ).list();

					assertEquals( 0, statistics.getQueryCacheHitCount() );
					assertEquals( 1, statistics.getQueryCachePutCount() );
					assertEquals( 1, statistics.getQueryCacheMissCount() );

					session.clear();

					session.createNamedQuery( "Dog.findAll", Dog.class ).list();
					assertEquals( 1, statistics.getQueryCacheHitCount() );
					assertEquals( 1, statistics.getQueryCachePutCount() );
					assertEquals( 1, statistics.getQueryCacheMissCount() );

					session.clear();
					statistics.clear();

					scope.getSessionFactory().getCache().evictRegion( REGION );

					session.createNamedQuery( "Dog.findAll", Dog.class ).list();

					assertEquals( 0, statistics.getSecondLevelCacheHitCount() );
					assertEquals( 0, statistics.getQueryCacheHitCount() );
				}
		);

	}

	@BeforeEach
	public void setupData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					Dog yogi = new Dog( "Yogi" );
					yogi.nickNames.add( "The Yog" );
					yogi.nickNames.add( "Little Boy" );
					yogi.nickNames.add( "Yogaroni Macaroni" );
					Dog irma = new Dog( "Irma" );
					irma.nickNames.add( "Squirmy" );
					irma.nickNames.add( "Bird" );
					session.persist( yogi );
					session.persist( irma );
				}
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		scope.getSessionFactory().getCache().evictAll();
	}

	@Entity(name = "Dog")
	@NamedQuery(name = "Dog.findAll", query = QUERY,
			hints = {
					@QueryHint(name = "org.hibernate.cacheable", value = "true"),
					@QueryHint(name = "org.hibernate.cacheRegion", value = REGION)
			}
	)
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = REGION)
	public static class Dog {
		@Id
		private String name;

		@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = REGION)
		@ElementCollection
		private Set<String> nickNames = new HashSet<>();

		public Dog(String name) {
			this.name = name;
		}

		public Dog() {
		}
	}
}
