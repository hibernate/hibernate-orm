/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Andrea Boriero
 */
@Jpa(annotatedClasses =  {LimitExpressionTest.Person.class})
public class LimitExpressionTest {

	@Test
	@JiraKey(value = "HHH-11278")
	public void testAnEmptyListIsReturnedWhenSetMaxResultsToZero(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaQuery<Person> query = entityManager.getCriteriaBuilder().createQuery( Person.class );
			query.from( Person.class );
			final List<Person> list = entityManager.createQuery( query ).setMaxResults( 0 ).getResultList();
			Assertions.assertTrue( list.isEmpty(), "The list should be empty with setMaxResults 0" );
		} );
	}

	@BeforeEach
	public void prepareTest(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person p = new Person();
			entityManager.persist( p );
		} );
	}

	@AfterEach
	public void cleanupTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;
	}
}
