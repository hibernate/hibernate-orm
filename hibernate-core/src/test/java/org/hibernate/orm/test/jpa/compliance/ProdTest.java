/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.dialect.CockroachDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Jpa(
		annotatedClasses = ProdTest.Person.class
)
public class ProdTest {
	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( new Person( 1, "Luigi ", 42 ) );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "https://github.com/cockroachdb/cockroach/issues/82478")
	public void testCriteriaMod(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<Number> query = criteriaBuilder.createQuery( Number.class );
					final Root<Person> person = query.from( Person.class );
					query.select( criteriaBuilder.prod( person.get( "age" ), 1F ) );

					final Number id = entityManager.createQuery( query ).getSingleResult();

					assertInstanceOf( Float.class, id );
					assertEquals( 42F, id.floatValue() );
				}
		);
	}

	@Test
	public void testQueryMod(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final Object id = entityManager.createQuery( "select p.age * 1F from Person p" )
							.getSingleResult();
					assertInstanceOf( Float.class, id );
					assertEquals( 42F, id );
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
