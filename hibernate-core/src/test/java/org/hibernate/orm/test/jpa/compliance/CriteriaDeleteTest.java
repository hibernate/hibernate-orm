/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = CriteriaDeleteTest.Person.class,
		jpaComplianceEnabled = true
)
public class CriteriaDeleteTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( new Person( 1, "Andrea", 5 ) );
					entityManager.persist( new Person( 2, "Fab", 40 ) );
				}
		);
	}

	@Test
	public void testModifyingDeleteQueryWhere(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaDelete<Person> criteriaDelete = criteriaBuilder
							.createCriteriaDelete( Person.class );
					final Root<Person> Person = criteriaDelete.from( Person.class );
					criteriaDelete.where( criteriaBuilder.lt( Person.get( "age" ), 35 ) );

					final Query q = entityManager.createQuery( criteriaDelete );
					criteriaDelete.where( criteriaBuilder.lt( Person.get( "age" ), 500 ) );

					assertEquals( 1, q.executeUpdate() );
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		private Integer age;

		Person() {
		}

		public Person(Integer id, String name, Integer age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

}
