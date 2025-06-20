/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				NullEmbeddableTest.EntityA.class
		}
)
@JiraKey("HHH-17290")
public class NullEmbeddableTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testPersist(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					EntityA entityA = new EntityA( "1", null );
					entityManager.persist( entityA );
				}
		);

		scope.inTransaction(
				entityManager -> {
					EntityA entityA = entityManager.find( EntityA.class, "1" );
					assertThat( entityA.materialCost ).isNull();
				}
		);
	}

	@Test
	public void testUpdate(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					EntityA entityA = new EntityA( "1", null );
					entityManager.persist( entityA );
				}
		);

		scope.inTransaction(
				entityManager -> {
					EntityA entityA = entityManager.find( EntityA.class, "1" );
					entityA.materialCost = null;
				}
		);

		scope.inTransaction(
				entityManager -> {
					EntityA entityA = entityManager.find( EntityA.class, "1" );
					assertThat( entityA.materialCost ).isNull();
				}
		);
	}

	@Test
	public void testMerge(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					EntityA entityA = new EntityA( "1", new Cost( 2, "USD" ) );
					entityManager.persist( entityA );
				}
		);

		scope.inTransaction(
				entityManager -> {
					EntityA entityA = new EntityA( "1", null );
					entityManager.merge( entityA );
				}
		);

		scope.inTransaction(
				entityManager -> {
					EntityA entityA = entityManager.find( EntityA.class, "1" );
					assertThat( entityA.materialCost ).isNull();
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		String id;

		@Embedded
		Cost materialCost;

		public EntityA() {
		}

		public EntityA(String id, Cost materialCost) {
			this.id = id;
			this.materialCost = materialCost;
		}
	}

	@Embeddable
	public static class Cost {
		int amount;
		String currency;

		public Cost() {
		}

		public Cost(Integer amount, String currency) {
			this.amount = amount;
			this.currency = currency;
		}
	}
}
