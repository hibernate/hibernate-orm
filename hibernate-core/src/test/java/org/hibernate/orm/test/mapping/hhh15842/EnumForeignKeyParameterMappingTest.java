/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.hhh15842;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.Assertions.assertThat;

@Jpa(annotatedClasses = {
		EnumForeignKeyParameterMappingTest.EntityWithEnumPrimaryKey.class,
		EnumForeignKeyParameterMappingTest.EntityWithEnumForeignKey.class
})
@JiraKey(value = "HHH-15842")
public class EnumForeignKeyParameterMappingTest {

	List<EntityWithEnumPrimaryKey> entitiesWithEnumPk = new ArrayList<>();

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			EnumPK[] enumValues = EnumPK.values();
			for ( int i = 0; i < enumValues.length; i++ ) {
				EnumPK enumVal = enumValues[i];
				EntityWithEnumPrimaryKey entity = new EntityWithEnumPrimaryKey();
				entity.setId( enumVal );
				em.persist( entity );
				entitiesWithEnumPk.add( entity );

				em.flush();

				// Native query is used here because HHH-15842 could cause persisting that test entity to fail
				em.createNativeQuery( "INSERT INTO test_entity_enum_fk (id, enum_fk) VALUES (?1, ?2)" )
						.setParameter( 1, i )
						.setParameter( 2, enumVal.name() )
						.executeUpdate();
			}
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		entitiesWithEnumPk.clear();
		scope.inTransaction( em -> {
			em.createQuery( "delete from EntityWithEnumForeignKey" ).executeUpdate();
			em.createQuery( "delete from EntityWithEnumPrimaryKey" ).executeUpdate();
		} );
	}

	@Test
	public void testPersist(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			long i = EnumPK.values().length;
			for ( EntityWithEnumPrimaryKey entityWithEnumPk : entitiesWithEnumPk ) {
				EntityWithEnumForeignKey entityWithFk = new EntityWithEnumForeignKey();
				entityWithFk.setId( i );
				entityWithFk.setEntityWithEnumPk( entityWithEnumPk );
				em.persist( entityWithFk );
				i++;
			}
		} );
	}

	@Test
	public void testQueryByRelatedEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			for ( EntityWithEnumPrimaryKey entityWithEnumPk : entitiesWithEnumPk ) {
				TypedQuery<EntityWithEnumForeignKey> q = em.createQuery(
						"select e from EntityWithEnumForeignKey e where e.entityWithEnumPk = ?1",
						EntityWithEnumForeignKey.class
				).setParameter( 1, entityWithEnumPk );

				assertThat( q.getResultList() ).hasSize( 1 );
			}
		} );
	}

	@Test
	public void testQueryByRelatedEntityId(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			for ( EnumPK enumVal : EnumPK.values() ) {
				TypedQuery<EntityWithEnumForeignKey> q = em.createQuery(
						"select e from EntityWithEnumForeignKey e where e.entityWithEnumPk.id = ?1",
						EntityWithEnumForeignKey.class
				).setParameter( 1, enumVal );

				assertThat( q.getResultList() ).hasSize( 1 );
			}
		} );
	}

	public enum EnumPK {
		A, B
	}

	@Entity(name = "EntityWithEnumForeignKey")
	@Table(name = "test_entity_enum_fk")
	public static class EntityWithEnumForeignKey {

		@Id
		@Column(name = "id")
		private Long id;

		@ManyToOne
		@JoinColumn(name = "enum_fk")
		private EntityWithEnumPrimaryKey entityWithEnumPk;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public EntityWithEnumPrimaryKey getEntityWithEnumPk() {
			return entityWithEnumPk;
		}

		public void setEntityWithEnumPk(EntityWithEnumPrimaryKey entityWithEnumPk) {
			this.entityWithEnumPk = entityWithEnumPk;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EntityWithEnumForeignKey that = (EntityWithEnumForeignKey) o;
			return id.equals( that.id ) && entityWithEnumPk.equals( that.entityWithEnumPk );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, entityWithEnumPk );
		}
	}

	@Entity(name = "EntityWithEnumPrimaryKey")
	@Table(name = "test_entity_enum_pk")
	public static class EntityWithEnumPrimaryKey {

		@Id
		@Column(name = "id")
		@Enumerated(EnumType.STRING)
		private EnumPK id;

		public EnumPK getId() {
			return id;
		}

		public void setId(EnumPK id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EntityWithEnumPrimaryKey that = (EntityWithEnumPrimaryKey) o;
			return id == that.id;
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}

	}
}
