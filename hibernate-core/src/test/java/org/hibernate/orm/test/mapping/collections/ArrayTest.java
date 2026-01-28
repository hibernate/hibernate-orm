/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@SessionFactory
@DomainModel( annotatedClasses = {ArrayTest.Person.class} )
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArrayTest {

	@Test
	public void testLifecycle(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person person = new Person(1L);
			String[] phones = new String[2];
			phones[0] = "028-234-9876";
			phones[1] = "072-122-9876";
			person.setPhones(phones);
			session.persist(person);
		});
		scope.inTransaction( session -> {
			Person person = session.find(Person.class, 1L);
			String[] phones = new String[1];
			phones[0] = "072-122-9876";
			person.setPhones(phones);
		});
	}

	//tag::collections-array-as-basic-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String[] phones;

		//Getters and setters are omitted for brevity

	//end::collections-array-as-basic-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public String[] getPhones() {
			return phones;
		}

		public void setPhones(String[] phones) {
			this.phones = phones;
		}
	//tag::collections-array-as-basic-example[]
	}
	//end::collections-array-as-basic-example[]
}
