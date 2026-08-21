/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.KeyType;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@DomainModel(
		annotatedClasses = {
				CacheModeRefreshSessionTest.RefreshSessionItem.class,
				CacheModeRefreshSessionTest.RefreshSessionNaturalIdItem.class,
				CacheModeRefreshSessionTest.RefreshSessionParent.class,
				CacheModeRefreshSessionTest.RefreshSessionChild.class
		}
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = CacheSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = CacheSettings.USE_QUERY_CACHE, value = "true"),
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

	@Test
	void refreshSessionRefreshesManagedFindMultipleResults(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new RefreshSessionItem( 4L, "one" ) );
			session.persist( new RefreshSessionItem( 5L, "two" ) );
		} );

		scope.inTransaction( session -> {
			final var first = session.find( RefreshSessionItem.class, 4L );
			final var second = session.find( RefreshSessionItem.class, 5L );
			assertEquals( "one", first.name );
			assertEquals( "two", second.name );

			updateItem( session, 4L, "updated one" );
			updateItem( session, 5L, "updated two" );

			final var refreshResults = session.findMultiple(
					RefreshSessionItem.class,
					List.of( 4L, 5L ),
					CacheMode.REFRESH_SESSION
			);

			assertSame( first, refreshResults.get( 0 ) );
			assertSame( second, refreshResults.get( 1 ) );
			assertEquals( "updated one", first.name );
			assertEquals( "updated two", second.name );
			assertEquals( 1, first.version );
			assertEquals( 1, second.version );
		} );
	}

	@Test
	void refreshSessionRefreshesManagedMultiLoadResults(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new RefreshSessionItem( 6L, "one" ) );
			session.persist( new RefreshSessionItem( 7L, "two" ) );
		} );

		scope.inTransaction( session -> {
			final var first = session.find( RefreshSessionItem.class, 6L );
			final var second = session.find( RefreshSessionItem.class, 7L );
			assertEquals( "one", first.name );
			assertEquals( "two", second.name );

			updateItem( session, 6L, "updated one" );
			updateItem( session, 7L, "updated two" );

			final var refreshResults = session.byMultipleIds( RefreshSessionItem.class )
					.with( CacheMode.REFRESH_SESSION )
					.multiLoad( 6L, 7L );

			assertSame( first, refreshResults.get( 0 ) );
			assertSame( second, refreshResults.get( 1 ) );
			assertEquals( "updated one", first.name );
			assertEquals( "updated two", second.name );
			assertEquals( 1, first.version );
			assertEquals( 1, second.version );
		} );
	}

	@Test
	void refreshSessionRefreshesManagedNaturalIdResult(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist( new RefreshSessionNaturalIdItem( 1L, "code-1", "one" ) )
		);

		scope.inTransaction( session -> {
			final var item = session.find( RefreshSessionNaturalIdItem.class, "code-1", KeyType.NATURAL );
			assertEquals( "one", item.name );

			updateNaturalIdItem( session, 1L, "two" );

			final var refreshResult = session.find(
					RefreshSessionNaturalIdItem.class,
					"code-1",
					KeyType.NATURAL,
					CacheMode.REFRESH
			);

			assertSame( item, refreshResult );
			assertEquals( "one", item.name );

			final var refreshSessionResult = session.find(
					RefreshSessionNaturalIdItem.class,
					"code-1",
					KeyType.NATURAL,
					CacheMode.REFRESH_SESSION
			);

			assertSame( item, refreshSessionResult );
			assertEquals( "two", item.name );
			assertEquals( 1, item.version );
		} );
	}

	@Test
	void refreshSessionRefreshesManagedCacheableQueryResults(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> session.persist( new RefreshSessionItem( 8L, "one" ) ) );

		scope.inTransaction( session -> session.createSelectionQuery(
						"from RefreshSessionItem where id = :id",
						RefreshSessionItem.class
				)
				.setParameter( "id", 8L )
				.setCacheable( true )
				.getSingleResult()
		);
		assertEquals( 1, statistics.getQueryCachePutCount() );

		statistics.clear();

		scope.inTransaction( session -> {
			final var item = session.find( RefreshSessionItem.class, 8L );
			assertEquals( "one", item.name );

			updateItem( session, 8L, "two" );

			final var refreshSessionResult = session.createSelectionQuery(
							"from RefreshSessionItem where id = :id",
							RefreshSessionItem.class
					)
					.setParameter( "id", 8L )
					.setCacheable( true )
					.setCacheMode( CacheMode.REFRESH_SESSION )
					.getSingleResult();

			assertSame( item, refreshSessionResult );
			assertEquals( "two", item.name );
			assertEquals( 1, item.version );
		} );
		assertEquals( 0, statistics.getQueryCacheHitCount() );
		assertEquals( 1, statistics.getQueryCachePutCount() );
	}

	@Test
	void refreshSessionRefreshesCacheableCollectionElements(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parent = new RefreshSessionParent( 1L, "parent" );
			parent.addChild( new RefreshSessionChild( 1L, "child 1" ) );
			parent.addChild( new RefreshSessionChild( 2L, "child 2" ) );
			session.persist( parent );
		} );

		scope.inTransaction( session -> {
			final var parent = session.find( RefreshSessionParent.class, 1L );
			Hibernate.initialize( parent.children );
			assertEquals( 2, parent.children.size() );

			updateChild( session, 1L, "updated 1" );
			updateChild( session, 2L, "updated 2" );

			final var refreshedParent = session.createSelectionQuery(
							"select distinct p from RefreshSessionParent p left join fetch p.children where p.id = :id",
							RefreshSessionParent.class
					)
					.setParameter( "id", 1L )
					.setCacheMode( CacheMode.REFRESH_SESSION )
					.getSingleResult();

			assertSame( parent, refreshedParent );
			assertEquals(
					List.of( "updated 1", "updated 2" ),
					parent.children.stream().map( child -> child.name ).sorted().toList()
			);
		} );
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

	private static void updateNaturalIdItem(Session session, Long id, String name) {
		session.doWork( connection -> {
			try ( final var statement = connection.prepareStatement(
					"update refresh_session_natural_id_item set name = ?, version = version + 1 where id = ?"
			) ) {
				statement.setString( 1, name );
				statement.setLong( 2, id );
				statement.executeUpdate();
			}
		} );
	}

	private static void updateChild(Session session, Long id, String name) {
		session.doWork( connection -> {
			try ( final var statement = connection.prepareStatement(
					"update refresh_session_child set name = ? where id = ?"
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

	@Entity(name = "RefreshSessionNaturalIdItem")
	@Table(name = "refresh_session_natural_id_item")
	@Cacheable
	@NaturalIdCache
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class RefreshSessionNaturalIdItem {
		@Id
		private Long id;
		@NaturalId
		private String code;
		@Version
		private Integer version;
		private String name;

		RefreshSessionNaturalIdItem() {
		}

		RefreshSessionNaturalIdItem(Long id, String code, String name) {
			this.id = id;
			this.code = code;
			this.name = name;
		}
	}

	@Entity(name = "RefreshSessionParent")
	@Table(name = "refresh_session_parent")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class RefreshSessionParent {
		@Id
		private Long id;
		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		private List<RefreshSessionChild> children = new ArrayList<>();

		RefreshSessionParent() {
		}

		RefreshSessionParent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		void addChild(RefreshSessionChild child) {
			children.add( child );
			child.parent = this;
		}
	}

	@Entity(name = "RefreshSessionChild")
	@Table(name = "refresh_session_child")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class RefreshSessionChild {
		@Id
		private Long id;
		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private RefreshSessionParent parent;

		RefreshSessionChild() {
		}

		RefreshSessionChild(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
