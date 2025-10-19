/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				DerivedIdentifierTest.Person.class,
				DerivedIdentifierTest.PersonDetails.class
		}
)
public class DerivedIdentifierTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		Long personId = scope.fromTransaction( entityManager -> {
			Person person = new Person( "ABC-123" );

			PersonDetails details = new PersonDetails();
			details.setPerson( person );

			entityManager.persist( person );
			entityManager.persist( details );

			return person.getId();
		} );

		scope.inTransaction( entityManager -> {
			PersonDetails details = entityManager.find( PersonDetails.class, personId );
			assertThat( details ).isNotNull();
		} );
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
		@MapsId
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
		}
	}
}
