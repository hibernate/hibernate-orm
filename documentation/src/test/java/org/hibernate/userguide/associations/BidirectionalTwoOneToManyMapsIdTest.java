/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.associations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;

import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalTwoOneToManyMapsIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Address.class,
				PersonAddress.class
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person1 = new Person( "ABC-123" );
			Person person2 = new Person( "DEF-456" );

			Address address1 = new Address( "12th Avenue", "12A", "4005A" );
			Address address2 = new Address( "18th Avenue", "18B", "4007B" );

			entityManager.persist( person1 );
			entityManager.persist( person2 );

			entityManager.persist( address1 );
			entityManager.persist( address2 );

			person1.addAddress( address1 );
			person1.addAddress( address2 );

			person2.addAddress( address1 );

			entityManager.flush();

			person1.removeAddress( address1 );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String registrationNumber;

		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<PersonAddress> addresses = new ArrayList<>();

		public Person() {
		}

		public Person(String registrationNumber) {
			this.registrationNumber = registrationNumber;
		}

		public Long getId() {
			return id;
		}

		public List<PersonAddress> getAddresses() {
			return addresses;
		}

		public void addAddress(Address address) {
			PersonAddress personAddress = new PersonAddress( this, address );
			addresses.add( personAddress );
			address.getOwners().add( personAddress );
		}

		public void removeAddress(Address address) {
			for ( Iterator<PersonAddress> iterator = addresses.iterator(); iterator.hasNext(); ) {
				PersonAddress personAddress = iterator.next();
				if(personAddress.getPerson().equals(this) &&
						personAddress.getAddress().equals(address)) {
					iterator.remove();
					personAddress.getAddress().getOwners().remove(personAddress);
					personAddress.setPerson(null);
					personAddress.setAddress(null);
				}
			}
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Person person = (Person) o;
			return Objects.equals( registrationNumber, person.registrationNumber );
		}

		@Override
		public int hashCode() {
			return Objects.hash( registrationNumber );
		}
	}

	@Embeddable
	public static class PersonAddressId implements Serializable {

		private Long personId;

		private Long addressId;

		public PersonAddressId() {
		}

		public PersonAddressId(Long personId, Long addressId) {
			this.personId = personId;
			this.addressId = addressId;
		}

		public Long getPersonId() {
			return personId;
		}

		public Long getAddressId() {
			return addressId;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			PersonAddressId that = (PersonAddressId) o;
			return Objects.equals( personId, that.personId ) &&
					Objects.equals( addressId, that.addressId );
		}

		@Override
		public int hashCode() {
			return Objects.hash( personId, addressId );
		}
	}

	@Entity(name = "PersonAddress")
	public static class PersonAddress {

		@EmbeddedId
		private PersonAddressId id;

		@ManyToOne
		@MapsId("personId")
		private Person person;

		@ManyToOne
		@MapsId("addressId")
		private Address address;

		public PersonAddress() {
		}

		public PersonAddress(Person person, Address address) {
			this.person = person;
			this.address = address;
			this.id = new PersonAddressId( person.getId(), address.getId() );
		}

		public PersonAddressId getId() {
			return id;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			PersonAddress that = (PersonAddress) o;
			return Objects.equals( person, that.person ) &&
					Objects.equals( address, that.address );
		}

		@Override
		public int hashCode() {
			return Objects.hash( person, address );
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

		private String postalCode;

		@OneToMany(mappedBy = "address", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<PersonAddress> owners = new ArrayList<>();

		public Address() {
		}

		public Address(String street, String number, String postalCode) {
			this.street = street;
			this.number = number;
			this.postalCode = postalCode;
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

		public String getPostalCode() {
			return postalCode;
		}

		public List<PersonAddress> getOwners() {
			return owners;
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
			return Objects.equals( street, address.street ) &&
					Objects.equals( number, address.number ) &&
					Objects.equals( postalCode, address.postalCode );
		}

		@Override
		public int hashCode() {
			return Objects.hash( street, number, postalCode );
		}
	}


}
