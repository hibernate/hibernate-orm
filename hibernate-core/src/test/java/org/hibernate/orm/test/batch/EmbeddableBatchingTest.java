/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.List;
import java.util.Objects;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.internal.SessionImpl;
import org.hibernate.persister.entity.EntityPersister;

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
				EmbeddableBatchingTest.EntityA.class,
				EmbeddableBatchingTest.EntityB.class,
				EmbeddableBatchingTest.EntityC.class,
				EmbeddableBatchingTest.EntityD.class
		},
		integrationSettings = { @Setting(name = Environment.DEFAULT_BATCH_FETCH_SIZE, value = "2") }
)
@JiraKey(value = "HHH-15644")
class EmbeddableBatchingTest {

	private static final EntityC ENTITY_C = new EntityC( 2, true );
	private static final EntityB ENTITY_B = new EntityB( 1, 2f );
	private static final EntityD ENTITY_D = new EntityD( 3, (byte) 100 );

	private static final EmbeddableA EMBEDDABLE_A = new EmbeddableA( "b1", ENTITY_B );
	private static final EmbeddableB EMBEDDABLE_B = new EmbeddableB( (short) 3, new EmbeddableC( 4, ENTITY_C ) );

	private static final EntityA ENTITY_A = new EntityA( 4, "a", null, ENTITY_D, EMBEDDABLE_A, EMBEDDABLE_B );

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( ENTITY_B );
					entityManager.persist( ENTITY_C );
					entityManager.persist( ENTITY_D );
					entityManager.persist( ENTITY_A );
				}
		);
	}

	@Test
	void testSelect(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaQuery<EntityA> query = entityManager.getCriteriaBuilder()
							.createQuery( EntityA.class );
					query.from( EntityA.class );

					List<EntityA> results = entityManager.createQuery( query ).getResultList();
					assertThat( results.size() ).isEqualTo( 1 );

					EntityA entityA = results.get( 0 );

					assertThat( entityA ).isEqualTo( ENTITY_A );

					EntityEntry entry = ( (SessionImpl) entityManager ).getPersistenceContext().getEntry( entityA );
					Object[] loadedState = entry.getLoadedState();
					EntityPersister persister = ( (SessionImpl) entityManager ).getEntityPersister(
							"EntityA",
							entityA
					);

					assertThat( loadedState[persister.getPropertyIndex( "name" )] ).isEqualTo( entityA.name );
					assertThat( loadedState[persister.getPropertyIndex( "anotherName" )] ).isEqualTo( null );
					assertThat( loadedState[persister.getPropertyIndex( "entityD" )] ).isEqualTo( ENTITY_D );
					assertThat( loadedState[persister.getPropertyIndex( "embeddableA" )] ).isEqualTo( EMBEDDABLE_A );
					assertThat( loadedState[persister.getPropertyIndex( "embeddableB" )] ).isEqualTo( EMBEDDABLE_B );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {

		@Id
		private Integer id;

		private String name;

		private String anotherName;

		@OneToOne
		private EntityD entityD;

		@Embedded
		private EmbeddableA embeddableA;

		@Embedded
		private EmbeddableB embeddableB;

		public EntityA() {
		}

		public EntityA(
				Integer id,
				String name,
				String anotherName,
				EntityD entityD,
				EmbeddableA embeddableA,
				EmbeddableB embeddableB) {
			this.id = id;
			this.name = name;
			this.anotherName = anotherName;
			this.entityD = entityD;
			this.embeddableA = embeddableA;
			this.embeddableB = embeddableB;
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
			) && Objects.equals( anotherName, entityA.anotherName ) && Objects.equals(
					entityD,
					entityA.entityD
			) && Objects.equals( embeddableA, entityA.embeddableA ) && Objects.equals(
					embeddableB,
					entityA.embeddableB
			);
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name, anotherName, entityD, embeddableA, embeddableB );
		}
	}

	@Embeddable
	public static class EmbeddableA {

		private String aString;

		@OneToOne
		private EntityB entityB;

		public EmbeddableA() {
		}

		public EmbeddableA(String aString, EntityB entityB) {
			this.aString = aString;
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
			return Objects.equals( aString, that.aString ) && Objects.equals( entityB, that.entityB );
		}

		@Override
		public int hashCode() {
			return Objects.hash( aString, entityB );
		}
	}

	@Embeddable
	public static class EmbeddableB {

		private short aShort;

		@Embedded
		private EmbeddableC d;

		public EmbeddableB() {
		}

		public EmbeddableB(short aShort, EmbeddableC d) {
			this.aShort = aShort;
			this.d = d;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EmbeddableB that = (EmbeddableB) o;
			return aShort == that.aShort && Objects.equals( d, that.d );
		}

		@Override
		public int hashCode() {
			return Objects.hash( aShort, d );
		}
	}

	@Embeddable
	public static class EmbeddableC {

		private int anInteger;

		@OneToOne
		private EntityC entityC;

		public EmbeddableC() {
		}

		public EmbeddableC(int anInteger, EntityC entityC) {
			this.anInteger = anInteger;
			this.entityC = entityC;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EmbeddableC that = (EmbeddableC) o;
			return Objects.equals( anInteger, that.anInteger ) && Objects.equals( entityC, that.entityC );
		}

		@Override
		public int hashCode() {
			return Objects.hash( anInteger, entityC );
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {

		@Id
		private Integer id;

		private Float aFloat;

		public EntityB() {
		}

		public EntityB(Integer id, Float aFloat) {
			this.id = id;
			this.aFloat = aFloat;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EntityB entityB = (EntityB) o;
			return Objects.equals( id, entityB.id ) && Objects.equals( aFloat, entityB.aFloat );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, aFloat );
		}
	}

	@Entity(name = "EntityC")
	public static class EntityC {

		@Id
		private Integer id;

		private boolean aBoolean;

		public EntityC() {
		}

		public EntityC(Integer id, boolean aBoolean) {
			this.id = id;
			this.aBoolean = aBoolean;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EntityC entityC = (EntityC) o;
			return aBoolean == entityC.aBoolean && Objects.equals( id, entityC.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, aBoolean );
		}
	}

	@Entity(name = "EntityD")
	public static class EntityD {

		@Id
		private Integer id;

		private byte aByte;

		public EntityD() {
		}

		public EntityD(Integer id, byte aByte) {
			this.id = id;
			this.aByte = aByte;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EntityD entityD = (EntityD) o;
			return aByte == entityD.aByte && Objects.equals( id, entityD.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, aByte );
		}
	}

}
