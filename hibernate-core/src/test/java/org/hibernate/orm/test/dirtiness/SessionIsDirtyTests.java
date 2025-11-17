/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dirtiness;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DomainModel(annotatedClasses = {
		SessionIsDirtyTests.EntityA.class,
		SessionIsDirtyTests.EntityB.class,
		SessionIsDirtyTests.EntityC.class,
})
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "5"),
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19605")
public class SessionIsDirtyTests {
	@Test
	public void testBatchAndCacheDirtiness(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();
		cache.evictAllRegions();
		scope.inTransaction( session -> {
			final List<EntityA> resultList = session.createSelectionQuery(
					"select a from EntityA a order by a.id",
					EntityA.class
			).getResultList();
			assertThat( session.isDirty() ).isFalse();

			assertThat( resultList ).hasSize( 2 );
			final EntityA entityA1 = resultList.get( 0 );
			assertThat( entityA1.getId() ).isEqualTo( 1L );
			assertThat( entityA1.getName() ).isEqualTo( "A1" );
			assertThat( entityA1.getEntityB() ).isNull();

			final EntityA entityA2 = resultList.get( 1 );
			assertThat( entityA2.getId() ).isEqualTo( 2L );
			assertThat( entityA2.getName() ).isEqualTo( "A2" );
			assertThat( entityA2.getEntityB() ).isNotNull();
			assertThat( entityA2.getEntityB().getEntityA() ).isSameAs( entityA1 );

			entityA2.getEntityB().setName( "B1 updated" );
			assertThat( session.isDirty() ).isTrue();
		} );
	}

	@Test
	public void testLazyAssociationDirtiness(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<EntityC> resultList = session.createSelectionQuery(
					"select c from EntityC c order by c.id",
					EntityC.class
			).getResultList();
			assertThat( session.isDirty() ).isFalse();

			assertThat( resultList ).hasSize( 1 );
			final EntityC entityC = resultList.get( 0 );
			assertThat( entityC.getId() ).isEqualTo( 1L );
			assertThat( entityC.getName() ).isEqualTo( "C1" );
			assertThat( Hibernate.isInitialized( entityC.getEntityB() ) ).isFalse();

			entityC.getEntityB().setName( "B1 lazy updated" );
			assertThat( session.isDirty() ).isTrue();
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA1 = new EntityA( 1L, "A1" );
			final EntityA entityA2 = new EntityA( 2L, "A2" );
			final EntityB entityB = new EntityB( 1L, "B1" );
			entityB.entityA = entityA1;
			entityA2.entityB = entityB;
			session.persist( entityA1 );
			session.persist( entityA2 );
			session.persist( entityB );

			final EntityC entityC = new EntityC( 1L, "C1" );
			entityC.entityB = entityB;
			session.persist( entityC );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "EntityA")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class EntityA {
		@Id
		Long id;

		String name;

		@ManyToOne
		@JoinColumn(name = "entity_b")
		EntityB entityB;

		public EntityA() {
		}

		public EntityA(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public EntityB getEntityB() {
			return entityB;
		}
	}

	@Entity(name = "EntityB")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class EntityB {
		@Id
		Long id;

		String name;

		@ManyToOne
		@JoinColumn(name = "entity_a")
		EntityA entityA;

		public EntityB() {
		}

		public EntityB(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public EntityA getEntityA() {
			return entityA;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityC")
	static class EntityC {
		@Id
		Long id;

		String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "entity_b")
		EntityB entityB;

		public EntityC() {
		}

		public EntityC(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public EntityB getEntityB() {
			return entityB;
		}
	}
}
