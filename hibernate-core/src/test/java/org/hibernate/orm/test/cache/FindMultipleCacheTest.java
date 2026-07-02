/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.List;

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
	public void testFindMultiplePreservesModifiedStateForCachedEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CachedEntity( 1L, "originalName" ) );
		} );

		scope.inTransaction( session -> {
			CachedEntity entity = session.findMultiple( CachedEntity.class, List.of( 1L ) ).get( 0 );
			assertThat( entity ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "originalName" );
			entity.setName( "modifiedName" );

			CachedEntity entity2 = session.findMultiple( CachedEntity.class, List.of( 1L ) ).get( 0 );
			assertThat( entity2 ).isSameAs( entity );
			assertThat( entity2.getName() ).isEqualTo( "modifiedName" );
		} );
	}

	@Test
	public void testFindMultiplePreservesModifiedStateForNonCachedEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new NotCachedEntity( 1L, "originalName" ) );
		} );

		scope.inTransaction( session -> {
			NotCachedEntity entity = session.findMultiple( NotCachedEntity.class, List.of( 1L ) ).get( 0 );
			assertThat( entity ).isNotNull();
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
	public void testFindMultiplePreservesModifiedStateWhenMixedWithDatabaseLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CachedEntity( 1L, "originalName1" ) );
			session.persist( new CachedEntity( 2L, "originalName2" ) );
		} );

		scope.getSessionFactory().getCache().evictAllRegions();

		scope.inTransaction( session -> {
			CachedEntity entity1 = session.find( CachedEntity.class, 1L );
			assertThat( entity1.getName() ).isEqualTo( "originalName1" );
			entity1.setName( "modifiedName1" );

			List<CachedEntity> results = session.findMultiple( CachedEntity.class, List.of( 1L, 2L ) );

			assertThat( results ).hasSize( 2 );
			CachedEntity result1 = results.get( 0 );
			CachedEntity result2 = results.get( 1 );

			assertThat( result1 ).isSameAs( entity1 );
			assertThat( result1.getName() ).isEqualTo( "modifiedName1" );
			assertThat( result2.getName() ).isEqualTo( "originalName2" );
		} );
	}


	@Test
	public void testMultiLoadPreservesModifiedStateForCachedEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CachedEntity( 1L, "originalName" ) );
		} );

		scope.inTransaction( session -> {
			CachedEntity entity = session.byMultipleIds( CachedEntity.class ).multiLoad( 1L ).get( 0 );
			assertThat( entity ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "originalName" );
			entity.setName( "modifiedName" );

			CachedEntity entity2 = session.byMultipleIds( CachedEntity.class ).multiLoad( 1L ).get( 0 );
			assertThat( entity2 ).isSameAs( entity );
			assertThat( entity2.getName() ).isEqualTo( "modifiedName" );
		} );
	}

	@Test
	public void testMultiLoadPreservesModifiedStateWhenMixedWithDatabaseLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new CachedEntity( 1L, "originalName1" ) );
			session.persist( new CachedEntity( 2L, "originalName2" ) );
		} );

		scope.getSessionFactory().getCache().evictAllRegions();

		scope.inTransaction( session -> {
			CachedEntity entity1 = session.find( CachedEntity.class, 1L );
			assertThat( entity1.getName() ).isEqualTo( "originalName1" );
			entity1.setName( "modifiedName1" );

			List<CachedEntity> results = session.byMultipleIds( CachedEntity.class ).multiLoad( 1L, 2L );

			assertThat( results ).hasSize( 2 );
			CachedEntity result1 = results.get( 0 );
			CachedEntity result2 = results.get( 1 );

			assertThat( result1 ).isSameAs( entity1 );
			assertThat( result1.getName() ).isEqualTo( "modifiedName1" );
			assertThat( result2.getName() ).isEqualTo( "originalName2" );
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
