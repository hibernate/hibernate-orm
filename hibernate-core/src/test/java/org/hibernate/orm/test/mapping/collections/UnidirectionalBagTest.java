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

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = {UnidirectionalBagTest.Person.class, UnidirectionalBagTest.Phone.class} )
public class UnidirectionalBagTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::collections-unidirectional-bag-lifecycle-example[]
			Person person = new Person(1L);
			person.getPhones().add(new Phone(1L, "landline", "028-234-9876"));
			person.getPhones().add(new Phone(2L, "mobile", "072-122-9876"));
			entityManager.persist(person);
			//end::collections-unidirectional-bag-lifecycle-example[]
		});
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			person.getPhones().remove(0);
		});
	}

	//tag::collections-unidirectional-bag-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@OneToMany(cascade = CascadeType.ALL)
		private List<Phone> phones = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::collections-unidirectional-bag-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public List<Phone> getPhones() {
			return phones;
		}
	//tag::collections-unidirectional-bag-example[]
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		private String type;

		@Column(name = "`number`")
		private String number;

		//Getters and setters are omitted for brevity

		//end::collections-unidirectional-bag-example[]

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
	//tag::collections-unidirectional-bag-example[]
	}
	//end::collections-unidirectional-bag-example[]
}
