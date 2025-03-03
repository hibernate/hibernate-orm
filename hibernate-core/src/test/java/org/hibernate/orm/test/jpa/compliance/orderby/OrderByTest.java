/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.orderby;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import static org.junit.jupiter.api.Assertions.fail;

@Jpa(
		annotatedClasses = {OrderByTest.Person.class, OrderByTest.Address.class}
)
public class OrderByTest {

	@Test
	public void testIt(EntityManagerFactoryScope scope) {

		Address address1 = new Address(
				1,
				"Milano",
				"Roma",
				"00100"
		);
		Address address2 = new Address(
				2,
				"Romolo",
				"Bergamo",
				"24100"
		);
		Address address3 = new Address(
				3,
				"Milano",
				"Garbagnate",
				"20040"
		);

		List<Address> addresses = new ArrayList<>();
		addresses.add( address1 );
		addresses.add( address2 );
		addresses.add( address3 );


		Person person = new Person( 1, "Fab", addresses );

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( address1 );
					entityManager.persist( address2 );
					entityManager.persist( address3 );
					entityManager.persist( person );
				}
		);

		scope.inTransaction(
				entityManager -> {
					List<Address> expected = new ArrayList<>();
					expected.add( address2 );
					expected.add( address3 );
					expected.add( address1 );
					Person p = entityManager.find( Person.class, 1 );

					List<Address> actual = p.getAddresses();

					final int expectedSize = expected.size();
					if ( actual.size() == expectedSize ) {
						for ( int i = 0; i < expectedSize; i++ ) {
							if ( !expected.get( i ).equals( actual.get( i ) ) ) {
								fail( "The addresses are in the wrong order" );
							}
						}
					}
					else {
						fail( "Expected " + expectedSize + " addresses but retrieved " + actual.size() );
					}
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		@OneToMany
		@OrderBy("zipcode DESC")
		private List<Address> addresses = new ArrayList<>();

		Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Person(Integer id, String name, List<Address> addresses) {
			this.id = id;
			this.name = name;
			this.addresses = addresses;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Address> getAddresses() {
			return addresses;
		}

	}

	@Entity(name = "Address")
	public static class Address {

		@Id
		private Integer id;

		private String street;

		private String city;

		private String zipcode;

		Address() {
		}

		public Address(Integer id,String street, String city, String zipcode) {
			this.id = id;
			this.street = street;
			this.city = city;
			this.zipcode = zipcode;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}


		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Address address2 = (Address) o;
			return Objects.equals( street, address2.street ) && Objects.equals(
					city,
					address2.city
			) && Objects.equals( zipcode, address2.zipcode );
		}

		@Override
		public int hashCode() {
			return Objects.hash( street, city, zipcode );
		}
	}

}
