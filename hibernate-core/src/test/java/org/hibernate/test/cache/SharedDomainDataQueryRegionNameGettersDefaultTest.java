/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class SharedDomainDataQueryRegionNameGettersDefaultTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final String QUERY = "SELECT a FROM Dog a";
	private static final String REGION_NAME = "TheRegion";
	private static final String PREFIX = "test";

	@Test
	@TestForIssue( jiraKey = "HHH-13586")
	public void testCacheImplementorRegionMethods() {

		final CacheImplementor cache = new CacheImplementorUsingDefaults( sessionFactory().getCache() );
		final String defaultQueryResultsRegionName = cache.getDefaultQueryResultsCache().getRegion().getName();

		// before named QueryCacheRegion has been created.

		// cache.getCacheRegionNames() should return both
		// REGION_NAME and the default QueryCacheResultsRegion name.
		assertEquals( 2, cache.getCacheRegionNames().size() );
		assertTrue( cache.getCacheRegionNames().contains( REGION_NAME ) );
		assertTrue( cache.getCacheRegionNames().contains( defaultQueryResultsRegionName ) );

		assertEquals( 1, cache.getDomainDataRegionNames().size() );
		assertTrue( cache.getDomainDataRegionNames().contains( REGION_NAME ) );

		final DomainDataRegion domainDataRegion = cache.getDomainDataRegion( REGION_NAME );
		assertNotNull( domainDataRegion );
		assertSame( domainDataRegion, cache.getRegion( REGION_NAME ) );

		assertNull( cache.getRegion( "not a region name" ) );
		assertNull( cache.getDomainDataRegion( "not a region name" ) );
		assertNull( cache.getQueryResultsCacheStrictly( "not a region name" ) );

		// cache.getQueryCacheRegionNames() should not contain the
		// default QueryResultsRegion name.
		assertTrue( cache.getQueryCacheRegionNames().isEmpty() );
		// default QueryResultsRegion can be obtained by name from getRegion( defaultQueryResultsRegionName)
		assertSame(
				cache.getDefaultQueryResultsCache().getRegion(),
				cache.getRegion( defaultQueryResultsRegionName )
		);

		// There should not be a QueryResultsRegion named REGION_NAME until
		// the named query is executed.
		assertNull( cache.getQueryResultsCacheStrictly( REGION_NAME ) );
		// default QueryResultsRegion cannot be obtained by name from getQueryResultsCacheStrictly( defaultQueryResultsRegionName)
		assertNull( cache.getQueryResultsCacheStrictly( defaultQueryResultsRegionName ) );

		doInHibernate(
				this::sessionFactory, session -> {
					session.createNamedQuery( "Dog.findAll", Dog.class ).list();
				}
		);

		// after named QueryCacheRegion has been created.

		// cache.getCacheRegionNames() should return both
		// REGION_NAME and the default QueryCacheResultsRegion name.
		assertEquals( 2, cache.getCacheRegionNames().size() );
		assertTrue( cache.getCacheRegionNames().contains( REGION_NAME ) );
		assertTrue( cache.getCacheRegionNames().contains( defaultQueryResultsRegionName ) );

		assertEquals( 1, cache.getDomainDataRegionNames().size() );
		assertTrue( cache.getDomainDataRegionNames().contains( REGION_NAME ) );

		assertSame( domainDataRegion, cache.getRegion( REGION_NAME ) );
		assertSame( domainDataRegion, cache.getDomainDataRegion( REGION_NAME ) );

		// cache.getQueryCacheRegionNames() should contain REGION_NAME now;
		// it still should not contain the default QueryResultsRegion name.
		assertEquals( 1, cache.getQueryCacheRegionNames().size() );
		assertEquals( REGION_NAME, cache.getQueryCacheRegionNames().iterator().next() );
		assertNotNull( cache.getQueryResultsCacheStrictly( REGION_NAME ) );
		assertSame( cache.getQueryResultsCacheStrictly( REGION_NAME ), cache.getQueryResultsCache( REGION_NAME ) );
		assertEquals( REGION_NAME, cache.getQueryResultsCacheStrictly( REGION_NAME ).getRegion().getName() );

		// default QueryResultsRegion can still be obtained by name from getRegion( defaultQueryResultsRegionName)
		assertSame(
				cache.getDefaultQueryResultsCache().getRegion(),
				cache.getRegion( defaultQueryResultsRegionName )
		);

		// Now there is a DomainDataRegion and QueryResultsRegion named REGION_NAME.
		// Make sure that the same DomainDataRegion is returned by cache.getRegion( REGION_NAME ).
		assertSame( domainDataRegion, cache.getRegion( REGION_NAME ) );
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
					@QueryHint(name = "org.hibernate.cacheRegion", value = REGION_NAME)
			}
	)
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region= REGION_NAME)
	public static class Dog {
		@Id
		private String name;

		@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region= REGION_NAME)
		@ElementCollection
		private Set<String> nickNames = new HashSet<>();

		public Dog(String name) {
			this.name = name;
		}

		public Dog() {
		}
	}

	private static class CacheImplementorUsingDefaults implements CacheImplementor {
		private final CacheImplementor delegate;

		public CacheImplementorUsingDefaults(CacheImplementor delegate) {
			this.delegate = delegate;
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return delegate.getSessionFactory();
		}

		@Override
		public boolean containsEntity(Class entityClass, Serializable identifier) {
			return delegate.containsEntity( entityClass, identifier );
		}

		@Override
		public boolean containsEntity(String entityName, Serializable identifier) {
			return delegate.containsEntity( entityName, identifier );
		}

		@Override
		public void evictEntityData(Class entityClass, Serializable identifier) {
			delegate.evictEntityData( entityClass, identifier );
		}

		@Override
		public void evictEntityData(String entityName, Serializable identifier) {
			delegate.evictEntityData( entityName, identifier );
		}

		@Override
		public void evictEntityData(Class entityClass) {
			delegate.evictEntityData( entityClass );
		}

		@Override
		public void evictEntityData(String entityName) {
			delegate.evictEntityData( entityName );
		}

		@Override
		public void evictEntityData() {
			delegate.evictEntityData();
		}

		@Override
		public void evictNaturalIdData(Class entityClass) {
			delegate.evictNaturalIdData( entityClass );
		}

		@Override
		public void evictNaturalIdData(String entityName) {
			delegate.evictNaturalIdData( entityName );
		}

		@Override
		public void evictNaturalIdData() {
			delegate.evictNaturalIdData();
		}

		@Override
		public boolean containsCollection(String role, Serializable ownerIdentifier) {
			return delegate.containsCollection( role, ownerIdentifier );
		}

		@Override
		public void evictCollectionData(String role, Serializable ownerIdentifier) {
			delegate.evictCollectionData( role, ownerIdentifier );
		}

		@Override
		public void evictCollectionData(String role) {
			delegate.evictCollectionData( role );
		}

		@Override
		public void evictCollectionData() {
			delegate.evictCollectionData();
		}

		@Override
		public boolean containsQuery(String regionName) {
			return delegate.containsQuery( regionName );
		}

		@Override
		public void evictDefaultQueryRegion() {
			delegate.evictDefaultQueryRegion();
		}

		@Override
		public void evictQueryRegion(String regionName) {
			delegate.evictQueryRegion( regionName );
		}

		@Override
		public void evictQueryRegions() {
			delegate.evictQueryRegions();
		}

		@Override
		public void evictRegion(String regionName) {
			delegate.evictRegion( regionName );
		}

		@Override
		public RegionFactory getRegionFactory() {
			return delegate.getRegionFactory();
		}

		@Override
		public void prime(Set<DomainDataRegionConfig> cacheRegionConfigs) {
			delegate.prime( cacheRegionConfigs );
		}

		@Override
		public Region getRegion(String regionName) {
			return delegate.getRegion( regionName );
		}

		@Override
		public Set<String> getCacheRegionNames() {
			return delegate.getCacheRegionNames();
		}

		@Override
		public TimestampsCache getTimestampsCache() {
			return delegate.getTimestampsCache();
		}

		@Override
		public QueryResultsCache getDefaultQueryResultsCache() {
			return delegate.getDefaultQueryResultsCache();
		}

		@Override
		public QueryResultsCache getQueryResultsCache(String regionName) {
			return delegate.getQueryResultsCache( regionName );
		}

		@Override
		public QueryResultsCache getQueryResultsCacheStrictly(String regionName) {
			return delegate.getQueryResultsCacheStrictly( regionName );
		}

		@Override
		public void close() {
			delegate.close();
		}

		@Override
		public String[] getSecondLevelCacheRegionNames() {
			return delegate.getSecondLevelCacheRegionNames();
		}

		@Override
		public EntityDataAccess getEntityRegionAccess(NavigableRole rootEntityName) {
			return delegate.getEntityRegionAccess( rootEntityName );
		}

		@Override
		public NaturalIdDataAccess getNaturalIdCacheRegionAccessStrategy(NavigableRole rootEntityName) {
			return delegate.getNaturalIdCacheRegionAccessStrategy( rootEntityName );
		}

		@Override
		public CollectionDataAccess getCollectionRegionAccess(NavigableRole collectionRole) {
			return delegate.getCollectionRegionAccess( collectionRole );
		}

		@Override
		public Set<NaturalIdDataAccess> getNaturalIdAccessesInRegion(String legacyQualifiedRegionName) {
			return delegate.getNaturalIdAccessesInRegion( legacyQualifiedRegionName );
		}

		@Override
		public boolean contains(Class cls, Object primaryKey) {
			return delegate.contains( cls, primaryKey );
		}

		@Override
		public void evict(Class cls, Object primaryKey) {
			delegate.evict( cls, primaryKey );
		}

		@Override
		public void evict(Class cls) {
			delegate.evict( cls );
		}

		@Override
		public <T> T unwrap(Class<T> cls) {
			return delegate.unwrap( cls );
		}
	}
}
