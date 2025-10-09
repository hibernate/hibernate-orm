/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(annotatedClasses = {
		OptimisticLockingTest.Person.class,
		OptimisticLockingTest.Phone.class
})
public class OptimisticLockingTest {
	@AfterEach
	void tearDown(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(EntityManagerFactoryScope factoryScope) {
		var _phone = factoryScope.fromTransaction(entityManager -> {
			var person = new Person();
			person.setName("John Doe");
			entityManager.persist(person);
			var phone = new Phone();
			phone.setNumber("123-456-7890");
			phone.setPerson(person);
			entityManager.persist(phone);
			return phone;
		});

		factoryScope.inTransaction( entityManager -> {
			var person = entityManager.find(Person.class, _phone.getPerson().getId());
			person.setName(person.getName().toUpperCase());

			var phone = entityManager.find(Phone.class, _phone.getId());
			phone.setNumber(phone.getNumber().replace("-", " "));
		});
	}

	//tag::locking-optimistic-entity-mapping-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`name`")
		private String name;

		//tag::locking-optimistic-version-number-example[]
		@Version
		private long version;
		//end::locking-optimistic-version-number-example[]

		//Getters and setters are omitted for brevity

		//end::locking-optimistic-entity-mapping-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public long getVersion() {
			return version;
		}

		public void setVersion(long version) {
			this.version = version;
		}
	//tag::locking-optimistic-entity-mapping-example[]
	}
	//end::locking-optimistic-entity-mapping-example[]

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`number`")
		private String number;

		@ManyToOne
		private Person person;

		//tag::locking-optimistic-version-timestamp-example[]
		@Version
		private Date version;
		//end::locking-optimistic-version-timestamp-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		public Date getVersion() {
			return version;
		}
	}
}
