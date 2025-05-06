/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaDelete;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Jan Schatteman
 */
@Jpa(annotatedClasses = {
		NonSelectCriteriaTest.Person.class
})
public class NonSelectCriteriaTest {

	@Test
	public void testNonSelectCriteriaCreation(EntityManagerFactoryScope scope) {
		// Tests that a non-select criteria can be created without getting IllegalArgumentExceptions of the type
		// "Non-select queries cannot be typed"
		scope.inTransaction(
				entityManager -> {
					CriteriaDelete<Person> deleteCriteria = entityManager.getCriteriaBuilder().createCriteriaDelete( Person.class );
					deleteCriteria.from( Person.class );
					entityManager.createQuery( deleteCriteria ).executeUpdate();
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;
	}
}
