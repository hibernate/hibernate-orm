/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class SharedDomainDataAndQueryResultsTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final String QUERY = "SELECT a FROM Dog a";
	private static final String REGION = "TheRegion";
	private static final String PREFIX = "test";

	@Test
	@TestForIssue( jiraKey = "HHH-13586")
	public void testAllCachedStatistics() {

		final Statistics statistics = sessionFactory().getStatistics();
		statistics.clear();

		final SecondLevelCacheStatistics regionStatisticsDeprecated = statistics.getSecondLevelCacheStatistics(
				PREFIX + '.' + REGION
		);
		final CacheRegionStatistics regionStatistics = statistics.getCacheRegionStatistics( REGION );
		assertSame( regionStatistics, regionStatisticsDeprecated );

		final QueryStatistics queryStatistics = statistics.getQueryStatistics( QUERY );

		doInHibernate(
				this::sessionFactory, session -> {

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

		doInHibernate(
				this::sessionFactory, session -> {

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

		doInHibernate(
				this::sessionFactory, session -> {

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

		doInHibernate(
				this::sessionFactory, session -> {

					List<Dog> dogs = session.getNamedQuery( "Dog.findAll" ).list();

					assertEquals( 2, dogs.size() );

					// statistics.getSecondLevelCacheHitCount() only includes entity/collection hits
					assertEquals( 6, statistics.getSecondLevelCacheHitCount() );
					// statistics.getSecondLevelCachePutCount() only includes entity/collection puts
					assertEquals( 4, statistics.getSecondLevelCachePutCount() );
					// statistics.getSecondLevelCacheMissCount() only includes entity/collection misses
					assertEquals( 2, statistics.getSecondLevelCacheMissCount() );

					// regionStatistics includes hits/puts/misses for entities/collections/query results
					// Query results will be found in the cache.
					// The 2 Dog entities will be found in the cache.
					assertEquals( 7, regionStatistics.getHitCount() );
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
					assertEquals( 8, statistics.getSecondLevelCacheHitCount() );
					// statistics.getSecondLevelCachePutCount() only includes entity/collection puts
					assertEquals( 4, statistics.getSecondLevelCachePutCount() );
					// statistics.getSecondLevelCacheMissCount() only includes entity/collection misses
					assertEquals( 2, statistics.getSecondLevelCacheMissCount() );

					// regionStatistics includes hits/puts/misses for entities/collections/query results
					assertEquals( 9, regionStatistics.getHitCount() );
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

	@Test
	@TestForIssue( jiraKey = "HHH-13586")
	public void testCacheImplementorGetRegion() {
		rebuildSessionFactory();

		final CacheImplementor cache = sessionFactory().getCache();
		final Region domainDataRegion = cache.getRegion( REGION );
		assertTrue( DomainDataRegion.class.isInstance( domainDataRegion ) );
		assertEquals( REGION, domainDataRegion.getName() );

		// There should not be a QueryResultsRegion named REGION until
		// the named query is executed.
		assertNull( cache.getQueryResultsCacheStrictly( REGION ) );

		doInHibernate(
				this::sessionFactory, session -> {
					session.createNamedQuery( "Dog.findAll", Dog.class ).list();
				}
		);

		// No there should be a QueryResultsCache named REGION
		final QueryResultsCache queryResultsCache = cache.getQueryResultsCacheStrictly( REGION );
		assertNotNull( queryResultsCache );
		assertEquals( REGION, queryResultsCache.getRegion().getName() );

		// Now there is a DomainDataRegion and QueryResultsRegion named REGION.
		// Make sure that the same DomainDataRegion is returned by cache.getRegion( REGION ).
		assertSame( domainDataRegion, cache.getRegion( REGION ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13586")
	public void testEvictCaches() {

		final Statistics statistics = sessionFactory().getStatistics();
		statistics.clear();

		assertEquals( 0, statistics.getSecondLevelCacheHitCount() );
		assertEquals( 0, statistics.getSecondLevelCachePutCount() );
		assertEquals( 0, statistics.getSecondLevelCacheMissCount() );

		assertEquals( 0, statistics.getQueryCacheHitCount() );
		assertEquals( 0, statistics.getQueryCachePutCount() );
		assertEquals( 0, statistics.getQueryCacheMissCount() );

		doInHibernate(
				this::sessionFactory, session -> {

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

					sessionFactory().getCache().evictRegion( REGION );

					session.createNamedQuery( "Dog.findAll", Dog.class ).list();

					assertEquals( 0, statistics.getSecondLevelCacheHitCount() );
					assertEquals( 0, statistics.getQueryCacheHitCount() );
				}
		);

	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, true );
		ssrb.applySetting( AvailableSettings.USE_QUERY_CACHE, true );
		ssrb.applySetting( AvailableSettings.CACHE_REGION_PREFIX, PREFIX );
		ssrb.applySetting( AvailableSettings.CACHE_REGION_FACTORY, new CachingRegionFactory() );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Dog.class );
	}

	@Before
	public void setupData() {
		doInHibernate(
				this::sessionFactory, session -> {
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

	@After
	public void cleanupData() {
		doInHibernate(
				this::sessionFactory, session -> {
					List<Dog> dogs = session.createQuery( "from Dog", Dog.class ).getResultList();
					for ( Dog dog : dogs ) {
						session.delete( dog );
					}
				}
		);
	}

	@Entity(name = "Dog")
	@NamedQuery(name = "Dog.findAll", query = QUERY,
			hints = {
					@QueryHint(name = "org.hibernate.cacheable", value = "true"),
					@QueryHint(name = "org.hibernate.cacheRegion", value = REGION)
			}
	)
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region=REGION)
	public static class Dog {
		@Id
		private String name;

		@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region=REGION)
		@ElementCollection
		private Set<String> nickNames = new HashSet<>();

		public Dog(String name) {
			this.name = name;
		}

		public Dog() {
		}
	}
}
