/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				OneToManyBidirectionalTest.Person.class,
				OneToManyBidirectionalTest.Phone.class,
		}
)
public class OneToManyBidirectionalTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::associations-one-to-many-bidirectional-lifecycle-example[]
			Person person = new Person();
			Phone phone1 = new Phone( "123-456-7890" );
			Phone phone2 = new Phone( "321-654-0987" );

			person.addPhone( phone1 );
			person.addPhone( phone2 );
			entityManager.persist( person );
			entityManager.flush();

			person.removePhone( phone1 );
			//end::associations-one-to-many-bidirectional-lifecycle-example[]
		} );
	}

	//tag::associations-one-to-many-bidirectional-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Phone> phones = new ArrayList<>();

		//Getters and setters are omitted for brevity

		//end::associations-one-to-many-bidirectional-example[]

		public Person() {
		}

		public Person(Long id) {
			this.id = id;
		}

		public List<Phone> getPhones() {
			return phones;
		}

		//tag::associations-one-to-many-bidirectional-example[]
		public void addPhone(Phone phone) {
			phones.add( phone );
			phone.setPerson( this );
		}

		public void removePhone(Phone phone) {
			phones.remove( phone );
			phone.setPerson( null );
		}
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		@Column(name = "`number`", unique = true)
		private String number;

		@ManyToOne
		private Person person;

		//Getters and setters are omitted for brevity

		//end::associations-one-to-many-bidirectional-example[]

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}

		public Long getId() {
			return id;
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

		//tag::associations-one-to-many-bidirectional-example[]
		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Phone phone = (Phone) o;
			return Objects.equals( number, phone.number );
		}

		@Override
		public int hashCode() {
			return Objects.hash( number );
		}
	}
	//end::associations-one-to-many-bidirectional-example[]
}
