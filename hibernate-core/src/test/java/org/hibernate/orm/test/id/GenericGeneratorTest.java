/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@DomainModel(
		annotatedClasses = GenericGeneratorTest.TestEntity.class
)
@SessionFactory
public class GenericGeneratorTest {

	@Test
	public void testInsert(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( "1", "test" );
					session.persist( entity );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue(generator = "assigned")
		@GenericGenerator(name = "assigned",
				strategy = "assigned",
				parameters = {
						@Parameter(name = "entity_name",
								value = "ProductUUID"
						)
				}
		)
		private String id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(String id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
