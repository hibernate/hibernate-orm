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
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				OneToOneMapsIdTest.Person.class,
				OneToOneMapsIdTest.PersonDetails.class
		}
)
public class OneToOneMapsIdTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		//tag::identifiers-derived-mapsid-persist-example[]
		scope.inTransaction( entityManager -> {
			Person person = new Person( "ABC-123" );
			person.setId( 1L );
			entityManager.persist( person );

			PersonDetails personDetails = new PersonDetails();
			personDetails.setNickName( "John Doe" );
			personDetails.setPerson( person );

			entityManager.persist( personDetails );
		} );

		scope.inTransaction( entityManager -> {
			PersonDetails personDetails = entityManager.find( PersonDetails.class, 1L );

			assertThat( personDetails.getNickName() ).isEqualTo( "John Doe" );
		} );
		//end::identifiers-derived-mapsid-persist-example[]
	}

	//tag::identifiers-derived-mapsid[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@NaturalId
		private String registrationNumber;

		public Person() {
		}

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
	public static class PersonDetails {

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
