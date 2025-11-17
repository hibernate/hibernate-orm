/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */

@Jpa(
		annotatedClasses = { LimitExpressionTest.Person.class }
)
public class LimitExpressionTest {

	@Test
	@JiraKey(value = "HHH-11278")
	public void testAnEmptyListIsReturnedWhenSetMaxResultsToZero(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final jakarta.persistence.Query query = entityManager.createQuery( "from Person p" );
					final List list = query.setMaxResults( 0 ).getResultList();
					assertTrue( list.isEmpty(), "The list should be empty with setMaxResults 0" );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-17004")
	public void testLimitReuse(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TypedQuery<Person> query;
					query = entityManager.createQuery( "from Person m", Person.class);
					query.setMaxResults(10);
					assertEquals( 10, query.getResultList().size() );

					query = entityManager.createQuery("from Person m", Person.class);
					query.setMaxResults(10);
					query.setFirstResult(2);
					assertEquals( 8, query.getResultList().size() );

					query = entityManager.createQuery("from Person m", Person.class);
					query.setMaxResults(10);
					assertEquals( 10, query.getResultList().size() );
				}
		);
	}

	@BeforeAll
	public void prepareTest(EntityManagerFactoryScope scope) throws Exception {
		scope.inTransaction(
				entityManager -> {
					Person p;
					for ( int i = 0; i < 10; i++ ) {
						p = new Person();
						entityManager.persist( p );
					}
				}
		);
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) throws Exception {
		scope.inTransaction(
				entityManager -> entityManager.createQuery( "delete from Person" ).executeUpdate()
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;
	}

}
