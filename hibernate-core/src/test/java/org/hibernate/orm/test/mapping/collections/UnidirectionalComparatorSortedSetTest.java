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
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SortComparator;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {UnidirectionalComparatorSortedSetTest.Person.class, UnidirectionalComparatorSortedSetTest.Phone.class} )
public class UnidirectionalComparatorSortedSetTest {

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
			Set<Phone> phones = person.getPhones();
			assertEquals(2, phones.size());
			phones.remove(phones.iterator().next());
			assertEquals(1, phones.size());
		});
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			Set<Phone> phones = person.getPhones();
			assertEquals(1, phones.size());
		});
	}

	//tag::collections-unidirectional-sorted-set-custom-comparator-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@OneToMany(cascade = CascadeType.ALL)
		@SortComparator(ReverseComparator.class)
		private SortedSet<Phone> phones = new TreeSet<>();

		//Getters and setters are omitted for brevity

	//end::collections-unidirectional-sorted-set-custom-comparator-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public Set<Phone> getPhones() {
			return phones;
		}
	//tag::collections-unidirectional-sorted-set-custom-comparator-example[]
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

		@NaturalId
		@Column(name = "`number`")
		private String number;

		//Getters and setters are omitted for brevity

	//end::collections-unidirectional-sorted-set-custom-comparator-example[]

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

	//tag::collections-unidirectional-sorted-set-custom-comparator-example[]
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
	//end::collections-unidirectional-sorted-set-custom-comparator-example[]
}
