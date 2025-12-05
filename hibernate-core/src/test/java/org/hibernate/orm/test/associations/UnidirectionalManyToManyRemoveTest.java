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
import jakarta.persistence.ManyToMany;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				UnidirectionalManyToManyRemoveTest.Person.class,
				UnidirectionalManyToManyRemoveTest.Address.class,
		}
)
public class UnidirectionalManyToManyRemoveTest {

	@Test
	public void testRemove(EntityManagerFactoryScope scope) {
		try {
			final Long personId = scope.fromTransaction( entityManager -> {
				Person person1 = new Person();
				Person person2 = new Person();

				Address address1 = new Address( "12th Avenue", "12A" );
				Address address2 = new Address( "18th Avenue", "18B" );

				person1.getAddresses().add( address1 );
				person1.getAddresses().add( address2 );

				person2.getAddresses().add( address1 );

				entityManager.persist( person1 );
				entityManager.persist( person2 );

				return person1.id;
			} );

			scope.inTransaction( entityManager -> {
				Person person1 = entityManager.find( Person.class, personId );
				entityManager.remove( person1 );
			} );
		}
		catch (Exception expected) {
			//expected
		}
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;
		@ManyToMany(cascade = {CascadeType.ALL})
		private List<Address> addresses = new ArrayList<>();

		public Person() {
		}

		public List<Address> getAddresses() {
			return addresses;
		}
	}

	@Entity(name = "Address")
	public static class Address {

		@Id
		@GeneratedValue
		private Long id;

		private String street;

		@Column(name = "`number`")
		private String number;

		public Address() {
		}

		public Address(String street, String number) {
			this.street = street;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getStreet() {
			return street;
		}

		public String getNumber() {
			return number;
		}
	}
}
