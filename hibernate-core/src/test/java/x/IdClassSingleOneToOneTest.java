/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package x;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@DomainModel( annotatedClasses = {
		IdClassSingleOneToOneTest.EntityA.class,
		IdClassSingleOneToOneTest.EntityB.class,
} )
@SessionFactory
public class IdClassSingleOneToOneTest {

	@Test
	public void test(SessionFactoryScope scope) {
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

	@Entity( name = "EntityA" )
	static class EntityA {

		@Id
		private Integer id;

		@OneToOne( mappedBy = "entityA", fetch = FetchType.LAZY )
		private EntityB entityB;

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
}
