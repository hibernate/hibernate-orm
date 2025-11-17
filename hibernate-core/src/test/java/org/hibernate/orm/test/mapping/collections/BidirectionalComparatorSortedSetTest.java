/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SortComparator;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static  org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {BidirectionalComparatorSortedSetTest.Person.class, BidirectionalComparatorSortedSetTest.Phone.class} )
public class BidirectionalComparatorSortedSetTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person person = new Person(1L);
			entityManager.persist(person);
			person.addPhone(new Phone(1L, "landline", "028-234-9876"));
			person.addPhone(new Phone(2L, "mobile", "072-122-9876"));
		});
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			Set<Phone> phones = person.getPhones();
			assertEquals(2, phones.size());
			person.removePhone(phones.iterator().next());
			assertEquals(1, phones.size());
		});
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			Set<Phone> phones = person.getPhones();
			assertEquals(1, phones.size());
		});
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;
		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
		@SortComparator(ReverseComparator.class)
		private SortedSet<Phone> phones = new TreeSet<>();

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public Set<Phone> getPhones() {
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

	public static class ReverseComparator implements Comparator<Phone> {
		@Override
		public int compare(Phone o1, Phone o2) {
			return o2.compareTo(o1);
		}
	}

	@Entity(name = "Phone")
	public static class Phone implements Comparable<Phone> {

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
}
