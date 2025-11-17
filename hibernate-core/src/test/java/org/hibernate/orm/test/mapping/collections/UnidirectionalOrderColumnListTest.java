/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {UnidirectionalOrderColumnListTest.Person.class, UnidirectionalOrderColumnListTest.Phone.class} )
public class UnidirectionalOrderColumnListTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person(1L);
			person.getPhones().add(new Phone(1L, "landline", "028-234-9876"));
			person.getPhones().add(new Phone(2L, "mobile", "072-122-9876"));
			entityManager.persist(person);
		});
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			person.getPhones().remove(0);
		});
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		//tag::collections-unidirectional-ordered-list-order-column-example[]
		@OneToMany(cascade = CascadeType.ALL)
		@OrderColumn(name = "order_id")
		private List<Phone> phones = new ArrayList<>();
		//end::collections-unidirectional-ordered-list-order-column-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public List<Phone> getPhones() {
			return phones;
		}
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		private String type;

		@Column(name = "`number`")
		private String number;

		public Phone() {
		}

		public Phone(Long id, String type, String number) {
			this.id = id;
			this.type = type;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getType() {
			return type;
		}

		public String getNumber() {
			return number;
		}
	}
}
