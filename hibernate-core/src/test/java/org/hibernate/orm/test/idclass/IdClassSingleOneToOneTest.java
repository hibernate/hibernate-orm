/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@DomainModel( annotatedClasses = {
		IdClassSingleOneToOneTest.EntityA.class,
		IdClassSingleOneToOneTest.EntityB.class,
		IdClassSingleOneToOneTest.EntityC.class,
} )
@SessionFactory
public class IdClassSingleOneToOneTest {

	@Test
	@Jira(value = "https://hibernate.atlassian.net/browse/HHH-19688")
	public void testWithoutJoinColumn(SessionFactoryScope scope) {
		scope.getSessionFactory();
		scope.inTransaction( session -> {
			EntityA entityA = new EntityA(3);
			EntityB entityB = new EntityB( entityA );
			entityA.entityB = entityB;
			session.persist( entityA );
			session.persist( entityB );
			assertEquals( new EntityBId(3),
					session.getIdentifier( entityB ) );
		} );
		scope.inTransaction( session -> {
			EntityB entityB = session.find( EntityB.class, new EntityBId(3) );
			assertNotNull( entityB );
		} );
	}

	@Test
	@Jira(value = "https://hibernate.atlassian.net/browse/HHH-20147")
	public void testWithJoinColumn(SessionFactoryScope scope) {
		scope.getSessionFactory();
		scope.inTransaction( session -> {
			EntityA entityA = new EntityA(4);
			EntityC entityC = new EntityC( entityA );
			entityA.entityC = entityC;
			session.persist( entityA );
			session.persist( entityC );
			assertEquals( new EntityCId(4),
					session.getIdentifier( entityC ) );
		} );
		scope.inTransaction( session -> {
			EntityC entityC = session.find( EntityC.class, new EntityCId(4) );
			assertNotNull( entityC );
		} );
	}

	@Entity( name = "EntityA" )
	static class EntityA {

		@Id
		private Integer id;

		@OneToOne( mappedBy = "entityA", fetch = FetchType.LAZY )
		private EntityB entityB;

		@OneToOne( mappedBy = "entityA", fetch = FetchType.LAZY )
		private EntityC entityC;

		public EntityA() {
		}

		public EntityA(Integer id) {
			this.id = id;
		}

	}

	@IdClass( EntityBId.class )
	@Entity( name = "EntityB" )
	static class EntityB {

		@Id
		@OneToOne( fetch = FetchType.LAZY )
		private EntityA entityA;

		public EntityB() {
		}

		public EntityB(EntityA entityA) {
			this.entityA = entityA;
		}

	}

	@IdClass( EntityCId.class )
	@Entity( name = "EntityC" )
	static class EntityC {

		@Id
		@OneToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "entity_a_id" )
		private EntityA entityA;

		public EntityC() {
		}

		public EntityC(EntityA entityA) {
			this.entityA = entityA;
		}

	}

	static class EntityBId {
		private final Integer entityA;

		EntityBId() {
			entityA = null;
		}
		EntityBId(Integer entityA) {
			this.entityA = entityA;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof EntityBId entityBId
				&& Objects.equals( entityA, entityBId.entityA );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( entityA );
		}
	}

	static class EntityCId {
		private final Integer entityA;

		EntityCId() {
			entityA = null;
		}
		EntityCId(Integer entityA) {
			this.entityA = entityA;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof EntityCId entityCId
				&& Objects.equals( entityA, entityCId.entityA );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( entityA );
		}
	}
}
