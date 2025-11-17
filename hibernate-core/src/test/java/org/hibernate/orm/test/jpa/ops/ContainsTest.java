/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ops;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {
		ContainsTest.Person.class
})
public class ContainsTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		Person _person = scope.fromTransaction(
				entityManager -> {
					Person person = new Person();
					person.id = 1L;
					person.name = "John Doe";
					entityManager.persist( person );

					assertTrue( entityManager.contains( person ) );

					return person;
				}
		);
		scope.inTransaction(
				entityManager -> {
					assertFalse(entityManager.contains( _person ));
					Person person = entityManager.find( Person.class, 1L );
					assertTrue(entityManager.contains( person ));
				}
		);
	}

	@Entity(name = "PersonEntity")
	public static class Person {

		@Id
		private Long id;

		private String name;
	}

}
