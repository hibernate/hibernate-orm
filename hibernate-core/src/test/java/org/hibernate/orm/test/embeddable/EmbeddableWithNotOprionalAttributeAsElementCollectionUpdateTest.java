/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

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

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = EmbeddableWithNotOprionalAttributeAsElementCollectionUpdateTest.Person.class)
@SessionFactory
@JiraKey(value = "HHH-16297")
public class EmbeddableWithNotOprionalAttributeAsElementCollectionUpdateTest {

	private Person thePerson;

	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Set<Address> addresses = new HashSet<>( Arrays.asList(
					new Address( "via milano", "Roma" ),
					new Address( "via Cartesio", "Milano" )
			) );
			thePerson = new Person( 7242000, "Claude", addresses );
			session.persist( thePerson );
		} );
	}

	@Test
	public void removeElementAndAddNewOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person foundPerson = session.find( Person.class, thePerson.getId() );
			Assertions.assertThat( foundPerson ).isNotNull();
			Set<Address> addresses = foundPerson.getAddresses();
			addresses.remove( new Address( "via milano", "Roma" ) );
			addresses.add( new Address( "via Pierce", "Milano" ) );
			assertThat( addresses )
					.containsExactlyInAnyOrder(
							new Address( "via Pierce", "Milano" ),
							new Address( "via Cartesio", "Milano" )
					);
		} );
		scope.inTransaction( session -> {
			Person person = session.find( Person.class, thePerson.getId() );
			assertThat( person.getAddresses() )
					.containsExactlyInAnyOrder(
							new Address( "via Pierce", "Milano" ),
							new Address( "via Cartesio", "Milano" )
					);
		} );
	}

	@Entity(name = "Person")
	@Table(name = "Person")
	static class Person {
		@Id
		private Integer id;
		private String name;

		@ElementCollection(fetch = FetchType.EAGER)
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

	@Embeddable
	public static class Address {
		@Basic(optional = false)
		private String street;
		@Basic(optional = false)
		private String city;

		public Address() {
		}

		public Address(String street, String city) {
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
