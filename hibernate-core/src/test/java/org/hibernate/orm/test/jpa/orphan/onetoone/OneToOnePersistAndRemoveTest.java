/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetoone;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertNull;

@Jpa(annotatedClasses = {
		OneToOnePersistAndRemoveTest.AEntity.class,
		OneToOnePersistAndRemoveTest.BEntity.class
})
public class OneToOnePersistAndRemoveTest {

	@Test
	public void testPersistAndRemoveBothEntitiesInSameFlush(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			AEntity a = new AEntity( 1L, "a1", 1 );
			BEntity b = new BEntity( 1L, "b1", 1, a );
			em.persist( b );

			em.remove( b.a );
			em.remove( b );
			em.flush();

			assertNull( em.find( AEntity.class, 1L ) );
			assertNull( em.find( BEntity.class, 1L ) );
		} );
	}

	@Test
	public void testPersistAndRemoveOnlyOwnerInSameFlush(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			AEntity a = new AEntity( 2L, "a2", 2 );
			BEntity b = new BEntity( 2L, "b2", 2, a );
			em.persist( b );

			em.remove( b );
			em.flush();

			assertNull( em.find( BEntity.class, 2L ) );
		} );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Entity(name = "PersistRemove1x1_A")
	@Table(name = "PERSIST_REMOVE_1X1_A")
	public static class AEntity {
		@Id
		Long id;

		String name;

		int aValue;

		@OneToOne(mappedBy = "a")
		BEntity b;

		public AEntity() {
		}

		public AEntity(Long id, String name, int aValue) {
			this.id = id;
			this.name = name;
			this.aValue = aValue;
		}
	}

	@Entity(name = "PersistRemove1x1_B")
	@Table(name = "PERSIST_REMOVE_1X1_B")
	public static class BEntity {
		@Id
		Long id;

		String name;

		int bValue;

		@OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER, orphanRemoval = true)
		@JoinColumn(name = "FK_FOR_A")
		AEntity a;

		public BEntity() {
		}

		public BEntity(Long id, String name, int bValue, AEntity a) {
			this.id = id;
			this.name = name;
			this.bValue = bValue;
			this.a = a;
		}
	}
}
