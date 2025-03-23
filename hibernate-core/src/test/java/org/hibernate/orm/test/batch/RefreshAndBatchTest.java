/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.annotations.BatchSize;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = RefreshAndBatchTest.TestEntity.class
)
@JiraKey(value = "HHH-15851")
public class RefreshAndBatchTest {
	private static final Long ENTITY1_ID = 1l;
	private static final Long ENTITY2_ID = 2l;

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TestEntity entity1 = new TestEntity( ENTITY1_ID, "Entity 1" );
					entityManager.persist( entity1 );

					TestEntity entity2 = new TestEntity( ENTITY2_ID, "Entity 2" );
					entityManager.persist( entity2 );
				}
		);
	}


	@Test
	public void testRefresh(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TestEntity entity1 = entityManager.find( TestEntity.class, ENTITY1_ID );
					TestEntity entity2 = entityManager.getReference( TestEntity.class, ENTITY2_ID );

					assertEquals( ENTITY1_ID, entity1.getId() );
					assertEquals( ENTITY2_ID, entity2.getId() );

					entityManager.refresh( entity1 );

					assertEquals( ENTITY1_ID, entity1.getId() );
					assertEquals( ENTITY2_ID, entity2.getId() );
				}
		);
	}

	@Entity(name = "TestEntity")
	@BatchSize(size = 100)
	public static class TestEntity {
		@Id
		private Long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
