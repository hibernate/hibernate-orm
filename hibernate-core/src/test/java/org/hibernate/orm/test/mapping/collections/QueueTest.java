/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.NaturalId;
import org.hibernate.orm.test.mapping.collections.type.QueueType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {QueueTest.Person.class, QueueTest.Phone.class} )
public class QueueTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person(1L);
			person.getPhones().add(new Phone(1L, "landline", "028-234-9876"));
			person.getPhones().add(new Phone(2L, "mobile", "072-122-9876"));
			entityManager.persist(person);
		});
		scope.inTransaction( entityManager -> {
			//tag::collections-custom-collection-example[]
			Person person = entityManager.find(Person.class, 1L);
			Queue<Phone> phones = person.getPhones();
			Phone head = phones.peek();
			assertSame(head, phones.poll());
			assertEquals(1, phones.size());
			//end::collections-custom-collection-example[]
		});
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			person.getPhones().clear();
		});
	}

	//tag::collections-custom-collection-mapping-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@OneToMany(cascade = CascadeType.ALL)
		@CollectionType(type = QueueType.class )
		private Collection<Phone> phones = new LinkedList<>();

		//Constructors are omitted for brevity

	//end::collections-custom-collection-mapping-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

	//tag::collections-custom-collection-mapping-example[]
		public Queue<Phone> getPhones() {
			return (Queue<Phone>) phones;
		}
	}

	@Entity(name = "Phone")
	public static class Phone implements Comparable<Phone> {

		@Id
		private Long id;

		private String type;

		@NaturalId
		@Column(name = "`number`")
		private String number;

		//Getters and setters are omitted for brevity

	//end::collections-custom-collection-mapping-example[]

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

	//tag::collections-custom-collection-mapping-example[]
		@Override
		public int compareTo(Phone o) {
			return number.compareTo(o.getNumber());
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
	//end::collections-custom-collection-mapping-example[]
}
