/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {BasicTypeElementCollectionTest.Person.class} )
public class BasicTypeElementCollectionTest {

	@BeforeEach
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person();
			person.id = 1L;
			person.phones.add("027-123-4567");
			person.phones.add("028-234-9876");
			entityManager.persist(person);
		});
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testProxies(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Assertions.assertThrows(
					ClassCastException.class,
					() -> {
						//tag::collections-collection-proxy-usage-example[]
						Person person = entityManager.find(Person.class, 1L);
						//Throws java.lang.ClassCastException: org.hibernate.collection.internal.PersistentBag cannot be cast to java.util.ArrayList
						ArrayList<String> phones = (ArrayList<String>) person.getPhones();
						//end::collections-collection-proxy-usage-example[]
					}
			);
		});
	}

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			//tag::ex-collection-elemental-lifecycle[]
			person.getPhones().clear();
			person.getPhones().add("123-456-7890");
			person.getPhones().add("456-000-1234");
			//end::ex-collection-elemental-lifecycle[]
		});
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			//tag::collections-value-type-collection-remove-example[]
			person.getPhones().remove(0);
			//end::collections-value-type-collection-remove-example[]
		});
	}

	//tag::ex-collection-elemental-basic-model[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@ElementCollection
		@OrderColumn( name = "phone_position")
		private List<String> phones = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::ex-collection-elemental-basic-model[]

		public List<String> getPhones() {
			return phones;
		}
	//tag::ex-collection-elemental-basic-model[]
	}
	//end::ex-collection-elemental-basic-model[]
}
