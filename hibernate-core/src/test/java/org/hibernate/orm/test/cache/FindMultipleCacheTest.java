/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.List;

import org.hibernate.FindMultipleOption;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				FindMultipleCacheTest.CachedEntity.class,
				FindMultipleCacheTest.NotCachedEntity.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
		}
)
@SessionFactory(generateStatistics = true)
@JiraKey("HHH-20515")
public class FindMultipleCacheTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@Test
	public void testFindMultipleWithDisabledPreservesModifiedStateWith2LCPresent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CachedEntity( 1L, "originalName" ) );
		} );

		scope.inTransaction( session -> {
			session.find( CachedEntity.class, 1L );
		} );

		scope.inTransaction( session -> {
			CachedEntity entity = session.find( CachedEntity.class, 1L );
			entity.setName( "modifiedName" );

			List<CachedEntity> results = session.findMultiple(
					CachedEntity.class,
					List.of( 1L ),
					FindMultipleOption.SessionCheckMode.DISABLED
			);

			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ) ).isSameAs( entity );
			assertThat( results.get( 0 ).getName() ).isEqualTo( "modifiedName" );
		} );
	}

	@Test
	public void testFindMultipleWithDisabledAndUninitializedProxyInPersistenceContext(
			SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CachedEntity( 1L, "name1" ) );
		} );

		scope.inTransaction( session -> {
			session.find( CachedEntity.class, 1L );
		} );

		scope.inTransaction( session -> {
			CachedEntity proxy =
					session.getReference( CachedEntity.class, 1L );

			List<CachedEntity> results = session.findMultiple(
					CachedEntity.class,
					List.of( 1L ),
					FindMultipleOption.SessionCheckMode.DISABLED

			);

			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ) ).isSameAs( proxy );
		} );
	}

	@Test
	public void testFindMultipleWithEnabledPreservesModifiedStateWith2LCPresent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CachedEntity( 1L, "originalName" ) );
		} );

		scope.inTransaction( session -> {
			session.find( CachedEntity.class, 1L );
		} );

		scope.inTransaction( session -> {
			CachedEntity entity = session.find( CachedEntity.class, 1L );
			entity.setName( "modifiedName" );

			List<CachedEntity> results = session.findMultiple(
					CachedEntity.class,
					List.of( 1L ),
					FindMultipleOption.SessionCheckMode.ENABLED
			);

			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ) ).isSameAs( entity );
			assertThat( results.get( 0 ).getName() ).isEqualTo( "modifiedName" );
		} );
	}

	@Test
	public void testFindMultiplePreservesModifiedStateForNonCachedEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new NotCachedEntity( 1L, "originalName" ) );
		} );

		scope.inTransaction( session -> {
			NotCachedEntity entity = session.findMultiple( NotCachedEntity.class, List.of( 1L ) ).get( 0 );
			assertThat( entity.getName() ).isEqualTo( "originalName" );
			entity.setName( "modifiedName" );

			NotCachedEntity entity2 = session.findMultiple( NotCachedEntity.class, List.of( 1L ) ).get( 0 );
			assertThat( entity2 ).isSameAs( entity );
			assertThat( entity2.getName() ).isEqualTo( "modifiedName" );
		} );
	}

	@Test
	public void testFindMultipleRetrievesPersistedChangesFromCache(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CachedEntity( 1L, "originalName" ) );
		} );

		scope.inTransaction( session -> {
			CachedEntity entity = session.findMultiple( CachedEntity.class, List.of( 1L ) ).get( 0 );
			entity.setName( "persistedName" );
		} );

		scope.inTransaction( session -> {
			CachedEntity entity = session.findMultiple( CachedEntity.class, List.of( 1L ) ).get( 0 );
			assertThat( entity.getName() ).isEqualTo( "persistedName" );
		} );
	}

	@Test
	public void testFindMultipleWithDisabledPreservesModifiedStateFromDb(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CachedEntity( 1L, "originalName" ) );
		} );

		scope.inTransaction( session -> {
			CachedEntity entity = session.find( CachedEntity.class, 1L );

			entity.setName( "modifiedName" );

			List<CachedEntity> results = session.findMultiple(
					CachedEntity.class,
					List.of( 1L ),
					FindMultipleOption.SessionCheckMode.DISABLED
			);

			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ) ).isSameAs( entity );
			assertThat( results.get( 0 ).getName() ).isEqualTo( "modifiedName" );
		} );
	}

	@Test
	public void testFindMultipleWithDisabledReturnsDeletedEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CachedEntity( 1L, "name1" ) );
			session.persist( new CachedEntity( 2L, "name2" ) );
		} );

		scope.inTransaction( session -> {
			CachedEntity deletedEntity =
					session.find( CachedEntity.class, 1L );

			session.remove( deletedEntity );

			List<CachedEntity> results = session.findMultiple(
					CachedEntity.class,
					List.of( 1L, 2L ),
					FindMultipleOption.SessionCheckMode.DISABLED
			);

			CachedEntity returnedEntity = results.get( 0 );
			assertThat( results ).hasSize( 2 );

			assertThat( results.get( 0 ) ).isNull();
			assertThat( results.get( 1 ) ).isNotNull();
			assertThat( results.get( 1 ).getName() ).isEqualTo( "name2" );
		} );
	}

	@Entity(name = "CachedEntity")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class CachedEntity {
		@Id
		private Long id;

		@Basic
		private String name;

		public CachedEntity() {
		}

		public CachedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "NotCachedEntity")
	public static class NotCachedEntity {
		@Id
		private Long id;

		@Basic
		private String name;

		public NotCachedEntity() {
		}

		public NotCachedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
