/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.associations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class ManyToManyBidirectionalWithLinkEntityTest extends BaseEntityManagerFunctionalTestCase {

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
			//tag::associations-many-to-many-bidirectional-with-link-entity-lifecycle-example[]
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

			log.info( "Removing address" );
			person1.removeAddress( address1 );
			//end::associations-many-to-many-bidirectional-with-link-entity-lifecycle-example[]
		} );
	}

	//tag::associations-many-to-many-bidirectional-with-link-entity-example[]
	@Entity(name = "Person")
	public static class Person implements Serializable {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String registrationNumber;

		@OneToMany(
			mappedBy = "person",
			cascade = CascadeType.ALL,
			orphanRemoval = true
		)
		private List<PersonAddress> addresses = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::associations-many-to-many-bidirectional-with-link-entity-example[]

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

	//tag::associations-many-to-many-bidirectional-with-link-entity-example[]
		public void addAddress(Address address) {
			PersonAddress personAddress = new PersonAddress( this, address );
			addresses.add( personAddress );
			address.getOwners().add( personAddress );
		}

		public void removeAddress(Address address) {
			PersonAddress personAddress = new PersonAddress( this, address );
			address.getOwners().remove( personAddress );
			addresses.remove( personAddress );
			personAddress.setPerson( null );
			personAddress.setAddress( null );
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

	@Entity(name = "PersonAddress")
	public static class PersonAddress implements Serializable {

		@Id
		@ManyToOne
		private Person person;

		@Id
		@ManyToOne
		private Address address;

		//Getters and setters are omitted for brevity

	//end::associations-many-to-many-bidirectional-with-link-entity-example[]

		public PersonAddress() {
		}

		public PersonAddress(Person person, Address address) {
			this.person = person;
			this.address = address;
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

	//tag::associations-many-to-many-bidirectional-with-link-entity-example[]
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
	public static class Address implements Serializable {

		@Id
		@GeneratedValue
		private Long id;

		private String street;

		@Column(name = "`number`")
		private String number;

		private String postalCode;

		@OneToMany(
			mappedBy = "address",
			cascade = CascadeType.ALL,
			orphanRemoval = true
		)
		private List<PersonAddress> owners = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::associations-many-to-many-bidirectional-with-link-entity-example[]

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

	//tag::associations-many-to-many-bidirectional-with-link-entity-example[]
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
	//end::associations-many-to-many-bidirectional-with-link-entity-example[]
}
