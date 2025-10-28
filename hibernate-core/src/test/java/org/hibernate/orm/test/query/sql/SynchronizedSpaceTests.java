/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sql;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.jpa.HibernateHints.HINT_NATIVE_SPACES;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				SynchronizedSpaceTests.CachedEntity.class,
				SynchronizedSpaceTests.NonCachedEntity.class
		},
		overrideCacheStrategy = false
)
@SessionFactory
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.CACHE_REGION_FACTORY,
				provider = SynchronizedSpaceTests.CachingRegionFactoryProvider.class
		)
)
public class SynchronizedSpaceTests {

	public static class CachingRegionFactoryProvider implements SettingProvider.Provider<String> {

		@Override
		public String getSetting() {
			return CachingRegionFactory.class.getName();
		}
	}

	@Test
	public void testNonSyncedCachedScenario(SessionFactoryScope scope) {
		// CachedEntity updated by native-query without adding query spaces
		// 		- the outcome should be all cached data being invalidated

		checkUseCase(
				scope,
				"cached_entity",
				query -> {
				},
				// the 2 CachedEntity entries should not be there
				false
		);

		// and of course, let's make sure the update happened :)
		scope.inTransaction(
				session -> {
					session.createQuery( "from CachedEntity", CachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name ).isEqualTo( "updated" )
					);
				}
		);
	}

	private void checkUseCase(
			SessionFactoryScope scope,
			String table,
			Consumer<Query> updateQueryConfigurer,
			boolean shouldExistAfter) {

		checkUseCase(
				scope,
				(session) -> {
					final Query nativeQuery = session.createNativeQuery( "update " + table + " set name = 'updated'" );
					updateQueryConfigurer.accept( nativeQuery );
					return nativeQuery;
				},
				Query::executeUpdate,
				shouldExistAfter
		);
	}

	private void checkUseCase(
			SessionFactoryScope scope,
			Function<SessionImplementor, Query> queryProducer,
			Consumer<Query> executor,
			boolean shouldExistAfter) {

		// first, load both `CachedEntity` instances into the L2 cache
		loadAll( scope );

		final CacheImplementor cacheSystem = scope.getSessionFactory().getCache();

		// make sure they are there
		assertThat( cacheSystem.containsEntity( CachedEntity.class, 1 ) ).isTrue();
		assertThat( cacheSystem.containsEntity( CachedEntity.class, 2 ) ).isTrue();

		// create a query to update the specified table - allowing the passed consumer to register a space if needed
		scope.inTransaction(
				session -> {
					// notice the type is the JPA Query interface
					final Query nativeQuery = queryProducer.apply( session );
					executor.accept( nativeQuery );
				}
		);

		// see if the entries exist based on the expectation
		assertThat( cacheSystem.containsEntity( CachedEntity.class, 1 ) ).isEqualTo( shouldExistAfter );
		assertThat( cacheSystem.containsEntity( CachedEntity.class, 2 ) ).isEqualTo( shouldExistAfter );
	}

	@Test
	public void testSyncedCachedScenario(SessionFactoryScope scope) {
		final String tableName = "cached_entity";

		checkUseCase(
				scope,
				tableName,
				query -> ((NativeQuery<?>) query).addSynchronizedQuerySpace( tableName ),
				// the 2 CachedEntity entries should not be there
				false
		);

		// and of course, let's make sure the update happened :)
		scope.inTransaction(
				session -> {
					session.createQuery( "from CachedEntity", CachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name ).isEqualTo( "updated" )
					);
				}
		);
	}

	@Test
	public void testNonSyncedNonCachedScenario(SessionFactoryScope scope) {
		// NonCachedEntity updated by native-query without adding query spaces
		// 		- the outcome should be all cached data being invalidated

		checkUseCase(
				scope,
				"non_cached_entity",
				query -> {
				},
				// the 2 CachedEntity entries should not be there
				false
		);

		// and of course, let's make sure the update happened :)
		scope.inTransaction(
				session -> {
					session.createQuery( "from NonCachedEntity", NonCachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name ).isEqualTo( "updated" )
					);
				}
		);
	}

	@Test
	public void testSyncedNonCachedScenario(SessionFactoryScope scope) {
		// NonCachedEntity updated by native-query with query spaces
		// 		- the caches for CachedEntity are not invalidated - they are not affected by the specified query-space

		final String tableName = "non_cached_entity";

		checkUseCase(
				scope,
				tableName,
				query -> ((NativeQuery<?>) query).addSynchronizedQuerySpace( tableName ),
				// the 2 CachedEntity entries should still be there
				true
		);

		// and of course, let's make sure the update happened :)
		scope.inTransaction(
				session -> {
					session.createQuery( "from NonCachedEntity", NonCachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name ).isEqualTo( "updated" )
					);
				}
		);
	}

	@Test
	public void testSyncedNonCachedScenarioUsingHint(SessionFactoryScope scope) {
		// same as `#testSyncedNonCachedScenario`, but here using the hint

		final String tableName = "non_cached_entity";

		checkUseCase(
				scope,
				tableName,
				query -> query.setHint( HINT_NATIVE_SPACES, tableName ),
				// the 2 CachedEntity entries should still be there
				true
		);

		// and of course, let's make sure the update happened :)
		scope.inTransaction(
				session -> {
					session.createQuery( "from NonCachedEntity", NonCachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name ).isEqualTo( "updated" )
					);
				}
		);
	}

	@Test
	public void testSyncedNonCachedScenarioUsingHintWithCollection(SessionFactoryScope scope) {
		// same as `#testSyncedNonCachedScenario`, but here using the hint

		final String tableName = "non_cached_entity";
		final Set<String> spaces = new HashSet<>();
		spaces.add( tableName );

		checkUseCase(
				scope,
				tableName,
				query -> query.setHint( HINT_NATIVE_SPACES, spaces ),
				// the 2 CachedEntity entries should still be there
				true
		);

		// and of course, let's make sure the update happened :)
		scope.inTransaction(
				session -> {
					session.createQuery( "from NonCachedEntity", NonCachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name ).isEqualTo( "updated" )
					);
				}
		);
	}

	@Test
	public void testSyncedNonCachedScenarioUsingHintWithArray(SessionFactoryScope scope) {
		// same as `#testSyncedNonCachedScenario`, but here using the hint

		final String tableName = "non_cached_entity";
		final String[] spaces = {tableName};

		checkUseCase(
				scope,
				tableName,
				query -> query.setHint( HINT_NATIVE_SPACES, spaces ),
				// the 2 CachedEntity entries should still be there
				true
		);

		// and of course, let's make sure the update happened :)
		scope.inTransaction(
				session -> {
					session.createQuery( "from NonCachedEntity", NonCachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name ).isEqualTo( "updated" )
					);
				}
		);
	}

	@Test
	public void testSyncedNonCachedScenarioUsingAnnotationWithReturnClass(SessionFactoryScope scope) {
		checkUseCase(
				scope,
				(session) -> session.createNamedQuery( "NonCachedEntity_return_class" ),
				Query::getResultList,
				true
		);
	}

	@Test
	public void testSyncedNonCachedScenarioUsingAnnotationWithResultSetMapping(SessionFactoryScope scope) {
		checkUseCase(
				scope,
				(session) -> session.createNamedQuery( "NonCachedEntity_resultset_mapping" ),
				Query::getResultList,
				true
		);
	}

	@Test
	public void testSyncedNonCachedScenarioUsingAnnotationWithSpaces(SessionFactoryScope scope) {
		checkUseCase(
				scope,
				(session) -> session.createNamedQuery( "NonCachedEntity_spaces" ),
				Query::getResultList,
				true
		);
	}

	@Test
	public void testSyncedNonCachedScenarioUsingJpaAnnotationWithNoResultMapping(SessionFactoryScope scope) {
		checkUseCase(
				scope,
				(session) -> session.createNamedQuery( "NonCachedEntity_raw_jpa" ),
				Query::getResultList,
				true
		);
	}

	@Test
	public void testSyncedNonCachedScenarioUsingJpaAnnotationWithHint(SessionFactoryScope scope) {
		checkUseCase(
				scope,
				(session) -> session.createNamedQuery( "NonCachedEntity_hint_jpa" ),
				Query::getResultList,
				true
		);
	}

	private void loadAll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from CachedEntity" ).list();

					// this one is not strictly needed since this entity is not cached.
					// but it helps my OCD feel better to have it ;)
					session.createQuery( "from NonCachedEntity" ).list();
				}
		);
	}

	@BeforeEach
	public void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new CachedEntity( 1, "first cached" ) );
					session.persist( new CachedEntity( 2, "second cached" ) );

					session.persist( new NonCachedEntity( 1, "first non-cached" ) );
					session.persist( new NonCachedEntity( 2, "second non-cached" ) );
				}
		);

		cleanupCache( scope );
	}

	@AfterEach
	public void cleanupTest(SessionFactoryScope scope) {
		cleanupCache( scope );
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	private void cleanupCache(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@Entity(name = "CachedEntity")
	@Table(name = "cached_entity")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class CachedEntity {
		@Id
		private Integer id;
		private String name;

		public CachedEntity() {
		}

		public CachedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "NonCachedEntity")
	@Table(name = "non_cached_entity")
	@Cacheable(false)
	@NamedNativeQuery(
			name = "NonCachedEntity_return_class",
			query = "select * from non_cached_entity",
			resultClass = NonCachedEntity.class
	)
	@NamedNativeQuery(
			name = "NonCachedEntity_resultset_mapping",
			query = "select * from non_cached_entity",
			resultSetMapping = "NonCachedEntity_resultset_mapping"
	)
	@SqlResultSetMapping(
			name = "NonCachedEntity_resultset_mapping",
			entities = @EntityResult(entityClass = NonCachedEntity.class)
	)
	@NamedNativeQuery(
			name = "NonCachedEntity_spaces",
			query = "select * from non_cached_entity",
			querySpaces = "non_cached_entity"
	)
	@jakarta.persistence.NamedNativeQuery(
			name = "NonCachedEntity_raw_jpa",
			query = "select * from non_cached_entity"
	)
	@jakarta.persistence.NamedNativeQuery(
			name = "NonCachedEntity_hint_jpa",
			query = "select * from non_cached_entity",
			hints = {
					@QueryHint(name = HINT_NATIVE_SPACES, value = "non_cached_entity")
			}
	)
	public static class NonCachedEntity {
		@Id
		private Integer id;
		private String name;

		public NonCachedEntity() {
		}

		public NonCachedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
