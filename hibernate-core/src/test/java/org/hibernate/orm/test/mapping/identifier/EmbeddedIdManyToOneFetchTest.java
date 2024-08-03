/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.identifier;

import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EmbeddedIdManyToOneFetchTest.EntityC.class,
		EmbeddedIdManyToOneFetchTest.EntityA.class,
		EmbeddedIdManyToOneFetchTest.EntityB.class,
		EmbeddedIdManyToOneFetchTest.AnotherEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18330" )
public class EmbeddedIdManyToOneFetchTest {
	@Test
	public void testInnerJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA result = session.createQuery(
					"select a from EntityA a " +
							"join fetch a.id.entityB b " +
							"join fetch b.id.entityC c", EntityA.class ).getSingleResult();
			final EntityB entityB = result.id.entityB;
			assertThat( entityB ).satisfies( Hibernate::isInitialized );
			assertThat( entityB.id.entityC ).satisfies( Hibernate::isInitialized );
		} );
	}

	@Test
	public void testLeftJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA result = session.createQuery(
					"select a from EntityA a " +
							"left join fetch a.id.entityB b " +
							"left join fetch b.id.entityC c", EntityA.class ).getSingleResult();
			final EntityB entityB = result.id.entityB;
			assertThat( entityB ).satisfies( Hibernate::isInitialized );
			assertThat( entityB.id.entityC ).satisfies( Hibernate::isInitialized );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityC entityC = new EntityC( "entity_c" );
			session.persist( entityC );
			final EntityB entityB = new EntityB( new EntityBId( entityC, "entity_b" ) );
			session.persist( entityB );
			final AnotherEntity anotherEntity = new AnotherEntity( 1L );
			session.persist( anotherEntity );
			session.persist( new EntityA( new EntityAId( entityB, anotherEntity ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.getSessionFactory().getSchemaManager().truncateMappedObjects() );
	}

	@Entity( name = "EntityA" )
	static class EntityA {
		@EmbeddedId
		private EntityAId id;

		public EntityA() {
		}

		public EntityA(EntityAId id) {
			this.id = id;
		}
	}

	@Embeddable
	static class EntityAId {
		@ManyToOne( fetch = FetchType.LAZY )
		private EntityB entityB;

		@ManyToOne( fetch = FetchType.LAZY )
		private AnotherEntity anotherEntity;

		public EntityAId() {
		}

		public EntityAId(EntityB entityB, AnotherEntity anotherEntity) {
			this.entityB = entityB;
			this.anotherEntity = anotherEntity;
		}
	}

	@Entity( name = "EntityB" )
	static class EntityB {
		@EmbeddedId
		private EntityBId id;

		public EntityB() {
		}

		public EntityB(EntityBId id) {
			this.id = id;
		}
	}

	@Embeddable
	static class EntityBId {
		@ManyToOne( fetch = FetchType.LAZY )
		private EntityC entityC;

		private String name;

		public EntityBId() {
		}

		public EntityBId(EntityC entityC, String name) {
			this.entityC = entityC;
			this.name = name;
		}
	}

	@Entity( name = "EntityC" )
	static class EntityC {
		@Id
		private String id;

		public EntityC() {
		}

		public EntityC(String id) {
			this.id = id;
		}
	}

	@Entity( name = "AnotherEntity" )
	static class AnotherEntity {
		@Id
		private Long id;

		public AnotherEntity() {
		}

		public AnotherEntity(Long id) {
			this.id = id;
		}
	}
}
