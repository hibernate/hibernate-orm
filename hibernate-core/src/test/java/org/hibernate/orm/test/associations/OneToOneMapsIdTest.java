/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.NaturalId;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class OneToOneMapsIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				PersonDetails.class
		};
	}

	@Test
	public void testLifecycle() {
		//tag::identifiers-derived-mapsid-persist-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = new Person("ABC-123");
			person.setId(1L);
			entityManager.persist(person);

			PersonDetails personDetails = new PersonDetails();
			personDetails.setNickName("John Doe");
			personDetails.setPerson(person);

			entityManager.persist(personDetails);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			PersonDetails personDetails = entityManager.find(PersonDetails.class, 1L);

			assertEquals("John Doe", personDetails.getNickName());
		});
		//end::identifiers-derived-mapsid-persist-example[]
	}

	//tag::identifiers-derived-mapsid[]
	@Entity(name = "Person")
	public static class Person  {

		@Id
		private Long id;

		@NaturalId
		private String registrationNumber;

		public Person() {}

		public Person(String registrationNumber) {
			this.registrationNumber = registrationNumber;
		}

		//Getters and setters are omitted for brevity
		//end::identifiers-derived-mapsid[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getRegistrationNumber() {
			return registrationNumber;
		}
	//tag::identifiers-derived-mapsid[]
	}

	@Entity(name = "PersonDetails")
	public static class PersonDetails  {

		@Id
		private Long id;

		private String nickName;

		@OneToOne
		@MapsId
		private Person person;

		//Getters and setters are omitted for brevity
		//end::identifiers-derived-mapsid[]

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
		}
	//tag::identifiers-derived-mapsid[]
	}
	//end::identifiers-derived-mapsid[]

}
