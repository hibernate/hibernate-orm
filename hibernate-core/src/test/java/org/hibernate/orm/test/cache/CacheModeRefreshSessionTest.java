/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@DomainModel(annotatedClasses = CacheModeRefreshSessionTest.RefreshSessionItem.class)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = CacheSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = CacheSettings.USE_MINIMAL_PUTS, value = "true"),
				@Setting(name = CacheSettings.CACHE_REGION_FACTORY,
						value = "org.hibernate.testing.cache.CachingRegionFactory")
		}
)
class CacheModeRefreshSessionTest {

	@Test
	void refreshSessionRefreshesManagedQueryResults(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new RefreshSessionItem( 1L, "one" ) ) );

		scope.inTransaction( session -> {
			final var item = session.find( RefreshSessionItem.class, 1L );
			assertEquals( "one", item.name );

			updateItem( session, 1L, "two" );

			final var refreshResult = session.createSelectionQuery(
							"from RefreshSessionItem where id = :id",
							RefreshSessionItem.class
					)
					.setParameter( "id", 1L )
					.setCacheMode( CacheMode.REFRESH )
					.getSingleResult();

			assertSame( item, refreshResult );
			assertEquals( "one", item.name );

			final var refreshSessionQuery = session.createSelectionQuery(
							"from RefreshSessionItem where id = :id",
							RefreshSessionItem.class
					)
					.setParameter( "id", 1L )
					.setCacheMode( CacheMode.REFRESH_SESSION );

			assertEquals( CacheMode.REFRESH_SESSION, refreshSessionQuery.getCacheMode() );
			assertEquals( CacheStoreMode.REFRESH, refreshSessionQuery.getCacheStoreMode() );
			assertEquals( CacheRetrieveMode.BYPASS, refreshSessionQuery.getCacheRetrieveMode() );

			final var refreshSessionResult = refreshSessionQuery.getSingleResult();

			assertSame( item, refreshSessionResult );
			assertEquals( "two", item.name );
			assertEquals( 1, item.version );
		} );
	}

	@Test
	void refreshSessionRefreshesManagedFindResult(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new RefreshSessionItem( 2L, "one" ) ) );

		scope.inTransaction( session -> {
			final var item = session.find( RefreshSessionItem.class, 2L );
			assertEquals( "one", item.name );

			updateItem( session, 2L, "two" );

			final var refreshResult = session.find( RefreshSessionItem.class, 2L, CacheMode.REFRESH );

			assertSame( item, refreshResult );
			assertEquals( "one", item.name );

			final var refreshSessionResult = session.find( RefreshSessionItem.class, 2L, CacheMode.REFRESH_SESSION );

			assertSame( item, refreshSessionResult );
			assertEquals( "two", item.name );
			assertEquals( 1, item.version );
		} );
	}

	@Test
	void refreshSessionForcesSecondLevelCacheRefresh(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> session.persist( new RefreshSessionItem( 3L, "one" ) ) );

		scope.inTransaction( session -> session.find( RefreshSessionItem.class, 3L, CacheMode.PUT ) );
		assertEquals( 1, statistics.getSecondLevelCachePutCount() );

		scope.inTransaction( session -> updateItem( session, 3L, "two" ) );

		scope.inTransaction( session -> session.find( RefreshSessionItem.class, 3L, CacheMode.REFRESH_SESSION ) );
		assertEquals( 2, statistics.getSecondLevelCachePutCount() );
	}

	private static void updateItem(Session session, Long id, String name) {
		session.doWork( connection -> {
			try ( final var statement = connection.prepareStatement(
					"update RefreshSessionItem set name = ?, version = version + 1 where id = ?"
			) ) {
				statement.setString( 1, name );
				statement.setLong( 2, id );
				statement.executeUpdate();
			}
		} );
	}

	@Entity(name = "RefreshSessionItem")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class RefreshSessionItem {
		@Id
		private Long id;
		@Version
		private Integer version;
		private String name;

		RefreshSessionItem() {
		}

		RefreshSessionItem(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
