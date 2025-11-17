/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.Collection;
import java.util.Set;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(
		annotatedClasses = {
				CriteriaGetCorrelatedJoinsTest.Person.class,
				CriteriaGetCorrelatedJoinsTest.Address.class
		}
)
public class CriteriaGetCorrelatedJoinsTest {

	@Test
	public void testGetCorrelatedJoins(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Person> query = criteriaBuilder.createQuery( Person.class );
					final Root<Person> person = query.from( Person.class );
					query.select( person );

					final Subquery<Address> subquery = query.subquery( Address.class );
					Set<Join<?, ?>> correlatedJoins = subquery.getCorrelatedJoins();
					assertNotNull( correlatedJoins );
					assertEquals( 0, correlatedJoins.size() );

					final Join<Person, Address> sqo = subquery
							.correlate( person.join( person.getModel().getCollection( "addresses", Address.class ) ) );
					subquery.select( sqo );

					correlatedJoins = subquery.getCorrelatedJoins();
					assertNotNull( correlatedJoins );
					assertEquals( 1, correlatedJoins.size() );
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

		@OneToMany
		private Collection<Address> addresses;

	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS_TABLE")
	public static class Address {

		@Id
		private Integer id;

		private String street;

		private String city;

		public Address(Integer id, String street, String city) {
			this.id = id;
			this.street = street;
			this.city = city;
		}
	}
}
