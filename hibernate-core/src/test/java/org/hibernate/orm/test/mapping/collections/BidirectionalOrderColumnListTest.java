/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {BidirectionalOrderColumnListTest.Person.class, BidirectionalOrderColumnListTest.Phone.class} )
public class BidirectionalOrderColumnListTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person(1L);
			entityManager.persist(person);
			person.addPhone(new Phone(1L, "landline", "028-234-9876"));
			person.addPhone(new Phone(2L, "mobile", "072-122-9876"));
			entityManager.flush();
			person.removePhone(person.getPhones().get(0));
		});
		scope.inTransaction( entityManager -> entityManager.find(Person.class, 1L).getPhones().size() );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		//tag::collections-bidirectional-ordered-list-order-column-example[]
		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
		@OrderColumn(name = "order_id")
		private List<Phone> phones = new ArrayList<>();
		//end::collections-bidirectional-ordered-list-order-column-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public List<Phone> getPhones() {
			return phones;
		}

		public void addPhone(Phone phone) {
			phones.add(phone);
			phone.setPerson(this);
		}

		public void removePhone(Phone phone) {
			phones.remove(phone);
			phone.setPerson(null);
		}
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		private String type;

		@Column(name = "`number`", unique = true)
		@NaturalId
		private String number;

		@ManyToOne
		private Person person;

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

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Phone phone = (Phone) o;
			return Objects.equals(number, phone.number);
		}

		@Override
		public int hashCode() {
			return Objects.hash(number);
		}
	}
}
