/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				OneToManyUpdateTest.Person.class,
				OneToManyUpdateTest.Address.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-16297")
public class OneToManyUpdateTest {

	private static final Integer PERSON_ID = 7242000;

	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Address address1 = new Address( 1, "via milano", "Roma" );
			Address address2 = new Address( 2, "via Cartesio", "Milano" );
			Set<Address> addresses = new HashSet<>( Arrays.asList(
					address1,
					address2
			) );
			Person person = new Person( PERSON_ID, "Claude", addresses );
			session.persist( address1 );
			session.persist( address2 );
			session.persist( person );
		} );
	}

	@Test
	public void removeElementAndAddNewOne(SessionFactoryScope scope) {
		Address address3 = new Address( 3, "via Pierce", "Milano" );
		Address address2 = new Address( 2, "via Cartesio", "Milano" );
		scope.inTransaction( session -> {
			Person foundPerson = session.find( Person.class, PERSON_ID );
			Assertions.assertThat( foundPerson ).isNotNull();
			Set<Address> addresses = foundPerson.getAddresses();
			Address address1 = new Address( 1, "via milano", "Roma" );
			addresses.remove( address1 );
			addresses.add( address3 );
			session.persist( address3 );
			assertThat( addresses )
					.containsExactlyInAnyOrder(
							address3,
							address2
					);
		} );
		scope.inTransaction( session -> {
			Person person = session.find( Person.class, PERSON_ID );
			Set<Address> addresses = person.getAddresses();
			assertThat( addresses )
					.containsExactlyInAnyOrder(
							address3,
							address2
					);
			addresses.remove( address2 );

		} );

		scope.inTransaction( session -> {
			Person person = session.find( Person.class, PERSON_ID );
			Set<Address> addresses = person.getAddresses();
			assertThat( addresses )
					.containsExactlyInAnyOrder(
							address3
					);
		} );
	}

	@Entity(name = "Person")
	@Table(name = "Person")
	static class Person {
		@Id
		private Integer id;
		private String name;

		@OneToMany(fetch = FetchType.EAGER)
		private Set<Address> addresses;

		public Person() {
		}

		public Person(Integer id, String name, Collection<Address> addresses) {
			this.id = id;
			this.name = name;
			this.addresses = new HashSet<>( addresses );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Address> getAddresses() {
			return addresses;
		}

		public void setAddresses(Set<Address> addresses) {
			this.addresses = addresses;
		}

	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS_TABLE")
	public static class Address {

		@Id
		private Integer id;

		private String street;

		private String city;

		public Address() {
		}

		public Address(Integer id, String street, String city) {
			this.id = id;
			this.street = street;
			this.city = city;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Address address = (Address) o;
			return Objects.equals( street, address.street ) && Objects.equals( city, address.city );
		}

		@Override
		public int hashCode() {
			return Objects.hash( street, city );
		}
	}
}
