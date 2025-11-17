/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				TreatTest.BaseEntity.class,
				TreatTest.EntityA.class,
				TreatTest.EntityB.class
		}
)
@JiraKey(value = "HHH-15721")
public class TreatTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					EntityB b = new EntityB( 1l, "b" );
					EntityA a = new EntityA( 1l, "a", b );

					EntityB b2 = new EntityB( 2l, "b2" );
					EntityA a2 = new EntityA( 2l, "a2", b2 );

					EntityB b3 = new EntityB( 3l, "b3" );
					EntityA a3 = new EntityA( 3l, "a3", b3 );

					entityManager.persist( b );
					entityManager.persist( a );

					entityManager.persist( b2 );
					entityManager.persist( a2 );

					entityManager.persist( b3 );
					entityManager.persist( a3 );
				}
		);
	}

	@Test
	public void testCriteriaQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<EntityA> query = cb.createQuery( EntityA.class );
					Root<BaseEntity> baseEntityRoot = query.from( BaseEntity.class );

					From<?, EntityA> entityARoot = cb.treat( baseEntityRoot, EntityA.class );
					Path<EntityB> entityB = entityARoot.join( "entityB", JoinType.LEFT );

					Predicate exp = cb.or(
							cb.equal( entityARoot.get( "name" ), "a2" ),
							cb.equal( entityB.get( "message" ), "b" )
					);

					query.select( cb.treat( baseEntityRoot, EntityA.class ) ).where( exp );
					List<EntityA> results = entityManager.createQuery( query ).getResultList();

					assertThat( results.size() ).isEqualTo( 2 );
				}
		);
	}

	@Entity(name = "BaseEntity")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static abstract class BaseEntity {
		@Id
		private Long id;

		public BaseEntity() {
		}

		public BaseEntity(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "EntityA")
	public static class EntityA extends BaseEntity {

		private String name;

		@ManyToOne
		private EntityB entityB;

		public EntityA() {
		}

		public EntityA(Long id, String name, EntityB entityB) {
			super( id );
			this.name = name;
			this.entityB = entityB;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {

		@Id
		private Long id;

		private String message;

		public EntityB() {
		}

		public EntityB(Long id, String message) {
			this.id = id;
			this.message = message;
		}
	}
}
