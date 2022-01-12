/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.compliance.elementcollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.collection.spi.PersistentList;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Jpa(
		annotatedClasses = { JpaListComplianceEnabledTest.Person.class },
		properties = @Setting(name = AvailableSettings.JPA_LIST_COMPLIANCE, value = "true")
)
public class JpaListComplianceEnabledTest {

	@Test
	public void testIt(EntityManagerFactoryScope scope) {

		Address address1 = new Address(
				"Milano",
				"Roma",
				"00100"
		);
		Address address2 = new Address(
				"Romolo",
				"Bergamo",
				"24100"
		);
		Address address3 = new Address(
				"Milano",
				"Garbagnate",
				"20040"
		);

		List<Address> addresses = new ArrayList<>();
		addresses.add( address1 );
		addresses.add( address2 );
		addresses.add( address3 );

		Person person = new Person( 1, "Fab", addresses, addresses );

		scope.inTransaction(
				entityManager ->
						entityManager.persist( person )
		);

		scope.inTransaction(
				entityManager -> {
					List<Address> expected = new ArrayList<>();
					expected.add( address1 );
					expected.add( address2 );
					expected.add( address3 );
					Person p = entityManager.find( Person.class, 1 );

					assertInstanceOf( PersistentBag.class, p.getAddresses() );
					assertInstanceOf( PersistentList.class, p.getOrderedAddresses() );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		@ElementCollection(fetch = FetchType.EAGER)
		private List<Address> addresses = new ArrayList<>();

		@ElementCollection(fetch = FetchType.EAGER)
		@OrderColumn
		private List<Address> orderedAddresses = new ArrayList<>();

		Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Person(Integer id, String name, List<Address> addresses, List<Address> orderedAddresses) {
			this.id = id;
			this.name = name;
			this.addresses = addresses;
			this.orderedAddresses = orderedAddresses;
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

		public List<Address> getOrderedAddresses() {
			return orderedAddresses;
		}
	}

	@Embeddable
	public static class Address {

		private String street;

		private String city;

		private String zipcode;

		Address() {
		}

		public Address(String street, String city, String zipcode) {
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
