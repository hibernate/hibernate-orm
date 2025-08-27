/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

import org.hibernate.annotations.NaturalId;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class DerivedIdentifierPrimaryKeyJoinColumnTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				PersonDetails.class
		};
	}

	@Test
	public void testLifecycle() {
		Long personId = doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = new Person("ABC-123");
			entityManager.persist(person);

			PersonDetails details = new PersonDetails();
			details.setPerson(person);

			entityManager.persist(details);
			return person.getId();
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			PersonDetails details = entityManager.find(PersonDetails.class, personId);
			Assert.assertNotNull(details);
		});
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String registrationNumber;

		public Person() {
		}

		public Person(String registrationNumber) {
			this.registrationNumber = registrationNumber;
		}

		public Long getId() {
			return id;
		}

		public String getRegistrationNumber() {
			return registrationNumber;
		}
	}

	@Entity(name = "PersonDetails")
	public static class PersonDetails {

		@Id
		private Long id;

		private String nickName;

		@ManyToOne
		@PrimaryKeyJoinColumn
		private Person person;

		public String getNickName() {
			return nickName;
		}

		public void setNickName(String nickName) {
			this.nickName = nickName;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
			this.id = person.getId();
		}
	}
}
