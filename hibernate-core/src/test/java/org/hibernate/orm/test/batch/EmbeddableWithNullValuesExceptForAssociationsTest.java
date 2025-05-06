/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.List;
import java.util.Objects;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.CriteriaQuery;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				EmbeddableWithNullValuesExceptForAssociationsTest.EntityA.class,
				EmbeddableWithNullValuesExceptForAssociationsTest.EntityB.class
		},
		integrationSettings = { @Setting(name = Environment.DEFAULT_BATCH_FETCH_SIZE, value = "2") }
)
@JiraKey( value = "HHH-15695")
public class EmbeddableWithNullValuesExceptForAssociationsTest {

	private static final EntityB ENTITY_B = new EntityB( 2, "d" );

	private static final EmbeddableA EMBEDDABLE_A = new EmbeddableA( null, ENTITY_B );

	private static final EntityA ENTITY_A = new EntityA( 4, "a", EMBEDDABLE_A );

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( ENTITY_B );
					entityManager.persist( ENTITY_A );
				}
		);
	}

	@Test
	public void testSelect(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaQuery<EntityA> query = entityManager.getCriteriaBuilder()
							.createQuery( EntityA.class );
					query.from( EntityA.class );

					List<EntityA> results = entityManager.createQuery( query ).getResultList();
					assertThat( results.size() ).isEqualTo( 1 );

					EntityA entityA = results.get( 0 );

					assertThat( entityA ).isEqualTo( ENTITY_A );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {

		@Id
		private Integer id;

		private String name;

		@Embedded
		private EmbeddableA embeddableA;

		public EntityA() {
		}

		public EntityA(Integer id, String name, EmbeddableA embeddableA) {
			this.id = id;
			this.name = name;
			this.embeddableA = embeddableA;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EntityA entityA = (EntityA) o;
			return Objects.equals( id, entityA.id ) && Objects.equals(
					name,
					entityA.name
			) && Objects.equals( embeddableA, entityA.embeddableA );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name, embeddableA );
		}
	}

	@Embeddable
	public static class EmbeddableA {

		private Integer anInt;

		@OneToOne
		private EntityB entityB;

		public EmbeddableA() {
		}

		public EmbeddableA(Integer anInt, EntityB entityB) {
			this.anInt = anInt;
			this.entityB = entityB;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EmbeddableA that = (EmbeddableA) o;
			return anInt == that.anInt && Objects.equals( entityB, that.entityB );
		}

		@Override
		public int hashCode() {
			return Objects.hash( anInt, entityB );
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {

		@Id
		private Integer id;

		private String aString;

		public EntityB() {
		}

		public EntityB(Integer id, String aString) {
			this.id = id;
			this.aString = aString;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EntityB entityD = (EntityB) o;
			return Objects.equals( id, entityD.id ) && Objects.equals(
					aString,
					entityD.aString
			);
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, aString );
		}
	}

}
