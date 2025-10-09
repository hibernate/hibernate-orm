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
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {BasicTypeOrderColumnElementCollectionTest.Person.class} )
public class BasicTypeOrderColumnElementCollectionTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person();
			person.id = 1L;
			person.getPhones().add("123-456-7890");
			person.getPhones().add("456-000-1234");
			entityManager.persist(person);
		});
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			//tag::collections-value-type-collection-order-column-remove-example[]
			person.getPhones().remove(0);
			//end::collections-value-type-collection-order-column-remove-example[]
		});
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		//tag::collections-value-type-collection-order-column-remove-entity-example[]
		@ElementCollection
		@OrderColumn(name = "order_id")
		private List<String> phones = new ArrayList<>();
		//end::collections-value-type-collection-order-column-remove-entity-example[]

		public List<String> getPhones() {
			return phones;
		}
	}
}
