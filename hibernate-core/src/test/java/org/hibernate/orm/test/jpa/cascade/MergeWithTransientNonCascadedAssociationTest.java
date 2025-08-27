/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;

import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {MergeWithTransientNonCascadedAssociationTest.Person.class,
						MergeWithTransientNonCascadedAssociationTest.Address.class})
public class MergeWithTransientNonCascadedAssociationTest {

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> entityManager.createQuery( "delete from Person" ).executeUpdate()
		);
	}

	@Test
	public void testMergeWithTransientNonCascadedAssociation(EntityManagerFactoryScope scope) {
		Person person = new Person();
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( person );
				}
		);

		person.address = new Address();

		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					entityManager.merge( person );
					try {
						entityManager.flush();
						fail( "Expecting IllegalStateException" );
					}
					catch (IllegalStateException ise) {
						// expected...
						entityManager.getTransaction().rollback();
					}
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue(generator = "increment")
		@GenericGenerator(name = "increment", strategy = "increment")
		private Integer id;
		@ManyToOne
		private Address address;

		public Person() {
		}
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		@GeneratedValue(generator = "increment_1")
		@GenericGenerator(name = "increment_1", strategy = "increment")
		private Integer id;
	}
}
