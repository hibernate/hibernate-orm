/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.associations;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class CompositeIdAssociationTest extends BaseEntityManagerFunctionalTestCase {

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
		PersonAddress _personAddress = doInJPA( this::entityManagerFactory, entityManager -> {
			Person person1 = new Person( "ABC-123" );
			Person person2 = new Person( "DEF-456" );

			Address address1 = new Address( "12th Avenue", "12A", "4005A" );
			Address address2 = new Address( "18th Avenue", "18B", "4007B" );

			entityManager.persist( person1 );
			entityManager.persist( person2 );

			entityManager.persist( address1 );
			entityManager.persist( address2 );

			PersonAddress personAddress = new PersonAddress( person1, address1 );
			entityManager.persist( personAddress );
			return personAddress;
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Address address = entityManager.createQuery( "from Address", Address.class ).getResultList().get( 0 );
			Person person = entityManager.createQuery( "from Person", Person.class ).getResultList().get( 0 );
			PersonAddress personAddress = entityManager.find(
					PersonAddress.class,
					new PersonAddress( person, address )
			);
		} );
	}

	@Entity(name = "PersonAddress")
	public static class PersonAddress implements Serializable {

		@Id
		@ManyToOne
		private Person person;

		@Id
		@ManyToOne()
		private Address address;

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

	@Entity(name = "Person")
	public static class Person implements Serializable {

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

	@Entity(name = "Address")
	public static class Address implements Serializable {

		@Id
		@GeneratedValue
		private Long id;

		private String street;

		@Column(name = "`number`")
		private String number;

		private String postalCode;

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
