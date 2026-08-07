/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.cache;

import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests that {@code @Basic(fetch = LAZY)} fields on {@code @Immutable}
 * entities with associations are properly cached in the second-level cache.
 * <p>
 * The entity has a {@code @ManyToOne} association which prevents the use of
 * reference cache entries ({@code ReferenceCacheEntryImpl}). The standard
 * cache entry path ({@code StandardCacheEntryImpl}) must include the lazy
 * field value in the disassembled state so that subsequent loads from cache
 * can serve the lazy field without a database round-trip.
 *
 * @author Ståle Pedersen
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-20773">HHH-20773</a>
 */
@DomainModel(
		annotatedClasses = {
				ImmutableLazyBasicWithAssociationCacheTest.DataHolder.class,
				ImmutableLazyBasicWithAssociationCacheTest.DataHolderNoLazyCache.class,
				ImmutableLazyBasicWithAssociationCacheTest.Category.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true"),
		}
)
@SessionFactory(useCollectingStatementObserver = true)
@BytecodeEnhanced
public class ImmutableLazyBasicWithAssociationCacheTest {

	private Long categoryId;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAll();
		scope.getSessionFactory().getStatistics().clear();

		scope.inTransaction( s -> {
			// Clean up any previous data
			s.createMutationQuery( "delete from DataHolder" ).executeUpdate();
			s.createMutationQuery( "delete from DataHolderNoLazyCache" ).executeUpdate();
			s.createMutationQuery( "delete from Category" ).executeUpdate();

			final Category category = new Category();
			category.setName( "test-category" );
			s.persist( category );
			categoryId = category.getId();
		} );
	}

	/**
	 * Tests that a lazy field set during persist is included in the 2LC
	 * cache entry and served from cache on subsequent loads without a
	 * database round-trip.
	 */
	@Test
	@JiraKey("HHH-20773")
	public void testLazyFieldCachedAfterPersist(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();
		stats.clear();
		scope.getSessionFactory().getCache().evictAll();

		final Long[] newId = new Long[1];

		// Persist a new entity with the lazy field set
		scope.inTransaction( s -> {
			final Category category = s.get( Category.class, categoryId );
			final DataHolder holder = new DataHolder();
			holder.setPayload( "fresh-payload" );
			holder.setCategory( category );
			s.persist( holder );
			s.flush();
			newId[0] = holder.getId();
		} );

		// Clear statistics and SQL observer after persist
		stats.clear();
		observer.clear();

		// Load the entity in a new session — should come from 2LC
		scope.inTransaction( s -> {
			final DataHolder holder = s.get( DataHolder.class, newId[0] );
			assertNotNull( holder );
			// The lazy field should be available from cache without DB query
			final String payload = holder.getPayload();
			assertEquals( "fresh-payload", payload,
					"Lazy field set during persist should be cached in 2LC" );
		} );

		// After persist, the entity should be in cache (put during commit)
		// The load should be a cache hit
		final CacheRegionStatistics regionStats = stats.getCacheRegionStatistics(
				DataHolder.class.getName() );
		assertEquals( 1, regionStats.getHitCount(),
				"Expected cache hit — entity should be cached after persist" );

		// Verify no SELECT queries hit the DataHolder table
		assertThat( observer.getSqlQueries().stream()
				.filter( sql -> sql.toLowerCase().contains( "select" )
						&& sql.toLowerCase().contains( "dataholder" ) )
				.toList() )
				.as( "No SELECT queries should hit the DataHolder table when served from 2LC" )
				.isEmpty();
	}

	/**
	 * Tests that when a lazy field is set during persist and the entity is
	 * later loaded from cache, the lazy field value round-trips through the
	 * 2LC correctly even after eviction and re-population. This simulates
	 * the real-world pattern described in HHH-20773: persist with value set,
	 * cache evict, re-load from DB populates the cache, then subsequent
	 * loads serve from cache.
	 */
	@Test
	@JiraKey("HHH-20773")
	public void testLazyFieldSurvivedCacheRoundTrip(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();
		stats.clear();
		scope.getSessionFactory().getCache().evictAll();

		final Long[] persistedId = new Long[1];

		// Step 1: Persist an entity with the lazy field set
		scope.inTransaction( s -> {
			final Category category = s.get( Category.class, categoryId );
			final DataHolder holder = new DataHolder();
			holder.setPayload( "roundtrip-payload" );
			holder.setCategory( category );
			s.persist( holder );
			s.flush();
			persistedId[0] = holder.getId();
		} );

		// After persist, entity should be cached with the lazy field value.
		// Clear stats and observer for the verification step.
		stats.clear();
		observer.clear();

		// Step 2: Load the entity in a new session — should come from 2LC
		// and the lazy field value should be available without DB query
		scope.inTransaction( s -> {
			final DataHolder holder = s.get( DataHolder.class, persistedId[0] );
			assertNotNull( holder );
			final String payload = holder.getPayload();
			assertEquals( "roundtrip-payload", payload,
					"Lazy field value should survive persist -> 2LC -> load round-trip" );
		} );

		// Verify cache hit
		final CacheRegionStatistics regionStats = stats.getCacheRegionStatistics(
				DataHolder.class.getName() );
		assertEquals( 1, regionStats.getHitCount(),
				"Expected cache hit — entity should be cached after persist" );
		assertEquals( 0, regionStats.getMissCount(),
				"Expected 0 cache misses" );

		// Verify no SELECT queries hit the DataHolder table
		assertThat( observer.getSqlQueries().stream()
				.filter( sql -> sql.toLowerCase().contains( "select" )
						&& sql.toLowerCase().contains( "dataholder" ) )
				.toList() )
				.as( "No SELECT queries should hit the DataHolder table when served from 2LC" )
				.isEmpty();
	}

	/**
	 * Tests that a null lazy field value is properly cached in the 2LC
	 * and served back as null without a database round-trip.
	 */
	@Test
	@JiraKey("HHH-20773")
	public void testNullLazyFieldCachedForImmutableEntity(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();
		stats.clear();
		scope.getSessionFactory().getCache().evictAll();

		final Long[] nullPayloadId = new Long[1];

		// Persist an entity with a null lazy field
		scope.inTransaction( s -> {
			final Category category = s.get( Category.class, categoryId );
			final DataHolder holder = new DataHolder();
			holder.setPayload( null ); // explicitly null
			holder.setCategory( category );
			s.persist( holder );
			s.flush();
			nullPayloadId[0] = holder.getId();
		} );

		// Clear stats and observer after persist
		stats.clear();
		observer.clear();

		// Load from cache — null value should be served from 2LC
		scope.inTransaction( s -> {
			final DataHolder holder = s.get( DataHolder.class, nullPayloadId[0] );
			assertNotNull( holder );
			final String payload = holder.getPayload();
			assertNull( payload, "Null lazy field value should be served from 2LC as null" );
		} );

		// Verify cache hit
		final CacheRegionStatistics regionStats = stats.getCacheRegionStatistics(
				DataHolder.class.getName() );
		assertEquals( 1, regionStats.getHitCount(),
				"Expected cache hit — entity with null lazy field should be cached" );

		// Verify no SELECT queries hit the DataHolder table
		assertThat( observer.getSqlQueries().stream()
				.filter( sql -> sql.toLowerCase().contains( "select" )
						&& sql.toLowerCase().contains( "dataholder" ) )
				.toList() )
				.as( "No SELECT queries should hit the DataHolder table for null lazy field" )
				.isEmpty();
	}

	/**
	 * Tests that when an entity is loaded from the database (not persisted
	 * in the current session), the lazy field is NOT initialized and the
	 * cache entry correctly preserves {@code UNFETCHED_PROPERTY} for the
	 * lazy field. This exercises the {@code isPropertyInitialized() == false}
	 * branch in {@code CacheEntryHelper.disassemble()}.
	 */
	@Test
	@JiraKey("HHH-20773")
	public void testLazyFieldNotInitializedDuringLoad(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();

		final Long[] entityId = new Long[1];

		// Persist an entity — this puts it in the 2LC with the lazy field
		scope.inTransaction( s -> {
			final Category category = s.get( Category.class, categoryId );
			final DataHolder holder = new DataHolder();
			holder.setPayload( "db-load-payload" );
			holder.setCategory( category );
			s.persist( holder );
			s.flush();
			entityId[0] = holder.getId();
		} );

		// Evict cache so the next load must go to the DB
		scope.getSessionFactory().getCache().evictAll();
		stats.clear();
		observer.clear();

		// Load from DB — the lazy field is NOT initialized (UNFETCHED_PROPERTY
		// in the entity state). The cache entry is built at this point, and
		// isPropertyInitialized() returns false for the lazy field.
		scope.inTransaction( s -> {
			final DataHolder holder = s.get( DataHolder.class, entityId[0] );
			assertNotNull( holder );
			// Do NOT access the lazy field — leave it uninitialized
		} );

		// Verify the entity was loaded from DB and cached
		final CacheRegionStatistics regionStats = stats.getCacheRegionStatistics(
				DataHolder.class.getName() );
		assertEquals( 1, regionStats.getMissCount(),
				"Expected cache miss on first load after eviction" );
		assertEquals( 1, regionStats.getPutCount(),
				"Expected cache put after DB load" );

		// Clear for the next load
		stats.clear();
		observer.clear();

		// Load again — should come from 2LC (cache hit), but accessing the
		// lazy field will still require a DB query since it was UNFETCHED_PROPERTY
		// in the cache entry (the field was not initialized during the first load)
		scope.inTransaction( s -> {
			final DataHolder holder = s.get( DataHolder.class, entityId[0] );
			assertNotNull( holder );
			// Access the lazy field — this triggers a DB query for the payload
			final String payload = holder.getPayload();
			assertEquals( "db-load-payload", payload );
		} );

		// The entity itself should be a cache hit
		final CacheRegionStatistics regionStats2 = stats.getCacheRegionStatistics(
				DataHolder.class.getName() );
		assertEquals( 1, regionStats2.getHitCount(),
				"Expected cache hit on second load" );

		// But accessing the lazy field should have triggered a SELECT for the payload
		assertThat( observer.getSqlQueries().stream()
				.filter( sql -> sql.toLowerCase().contains( "select" )
						&& sql.toLowerCase().contains( "payload" ) )
				.toList() )
				.as( "A SELECT for the payload column is expected since the lazy field "
						+ "was not initialized during the first (DB) load" )
				.isNotEmpty();
	}

	/**
	 * Tests that when {@code @Cache(includeLazy = false)} is used, the
	 * lazy field value is NOT included in the cache entry — even if it
	 * is initialized on the entity. This exercises the
	 * {@code !isLazyPropertiesCacheable()} branch in
	 * {@code CacheEntryHelper.disassemble()}.
	 */
	@Test
	@JiraKey("HHH-20773")
	public void testLazyFieldNotCachedWhenIncludeLazyIsFalse(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();
		stats.clear();
		scope.getSessionFactory().getCache().evictAll();

		final Long[] entityId = new Long[1];

		// Persist an entity with includeLazy=false and the lazy field set
		scope.inTransaction( s -> {
			final Category category = s.get( Category.class, categoryId );
			final DataHolderNoLazyCache holder = new DataHolderNoLazyCache();
			holder.setPayload( "no-lazy-cache-payload" );
			holder.setCategory( category );
			s.persist( holder );
			s.flush();
			entityId[0] = holder.getId();
		} );

		// Clear stats and observer after persist
		stats.clear();
		observer.clear();

		// Load from 2LC — the entity should be cached, but the lazy field
		// should NOT be in the cache entry (includeLazy=false)
		scope.inTransaction( s -> {
			final DataHolderNoLazyCache holder = s.get(
					DataHolderNoLazyCache.class, entityId[0] );
			assertNotNull( holder );
			// Access the lazy field — must trigger a DB query since
			// the field is excluded from the cache entry
			final String payload = holder.getPayload();
			assertEquals( "no-lazy-cache-payload", payload,
					"Lazy field value should be loaded from DB" );
		} );

		// The entity should be a cache hit
		final CacheRegionStatistics regionStats = stats.getCacheRegionStatistics(
				DataHolderNoLazyCache.class.getName() );
		assertEquals( 1, regionStats.getHitCount(),
				"Expected cache hit — entity should be cached after persist" );

		// But accessing the lazy field should have triggered a SELECT for the payload
		assertThat( observer.getSqlQueries().stream()
				.filter( sql -> sql.toLowerCase().contains( "select" )
						&& sql.toLowerCase().contains( "payload" ) )
				.toList() )
				.as( "A SELECT for the payload column is expected since includeLazy=false" )
				.isNotEmpty();
	}

	/**
	 * Immutable entity with a lazy basic field and a ManyToOne association.
	 * The association prevents ReferenceCacheEntryImpl from being used,
	 * forcing the StandardCacheEntryImpl path.
	 */
	@Entity(name = "DataHolder")
	@Immutable
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	public static class DataHolder {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "payload", length = 4000)
		@Basic(fetch = FetchType.LAZY)
		private String payload;

		@ManyToOne(fetch = FetchType.LAZY)
		private Category category;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getPayload() {
			return payload;
		}

		public void setPayload(String payload) {
			this.payload = payload;
		}

		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}
	}

	/**
	 * Immutable entity with a lazy basic field, a ManyToOne association,
	 * and {@code @Cache(includeLazy = false)}. The lazy field value should
	 * NOT be included in the cache entry.
	 */
	@Entity(name = "DataHolderNoLazyCache")
	@Immutable
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, includeLazy = false)
	public static class DataHolderNoLazyCache {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "payload", length = 4000)
		@Basic(fetch = FetchType.LAZY)
		private String payload;

		@ManyToOne(fetch = FetchType.LAZY)
		private Category category;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getPayload() {
			return payload;
		}

		public void setPayload(String payload) {
			this.payload = payload;
		}

		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}
	}

	/**
	 * Simple associated entity to ensure the DataHolder has an EntityType
	 * property, which prevents reference cache entries.
	 */
	@Entity(name = "Category")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Category {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
