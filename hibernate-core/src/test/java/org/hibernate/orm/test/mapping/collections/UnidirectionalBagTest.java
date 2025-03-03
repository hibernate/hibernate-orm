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

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class UnidirectionalBagTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::collections-unidirectional-bag-lifecycle-example[]
			Person person = new Person(1L);
			person.getPhones().add(new Phone(1L, "landline", "028-234-9876"));
			person.getPhones().add(new Phone(2L, "mobile", "072-122-9876"));
			entityManager.persist(person);
			//end::collections-unidirectional-bag-lifecycle-example[]
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
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
