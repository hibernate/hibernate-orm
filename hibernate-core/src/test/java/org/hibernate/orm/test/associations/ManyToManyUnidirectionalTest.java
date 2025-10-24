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
				ManyToManyUnidirectionalTest.Person.class,
				ManyToManyUnidirectionalTest.Address.class,
		}
)
public class ManyToManyUnidirectionalTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::associations-many-to-many-unidirectional-lifecycle-example[]
			Person person1 = new Person();
			Person person2 = new Person();

			Address address1 = new Address( "12th Avenue", "12A" );
			Address address2 = new Address( "18th Avenue", "18B" );

			person1.getAddresses().add( address1 );
			person1.getAddresses().add( address2 );

			person2.getAddresses().add( address1 );

			entityManager.persist( person1 );
			entityManager.persist( person2 );

			entityManager.flush();

			person1.getAddresses().remove( address1 );
			//end::associations-many-to-many-unidirectional-lifecycle-example[]
		} );
	}

	@Test
	public void testRemove(EntityManagerFactoryScope scope) {
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
			//tag::associations-many-to-many-unidirectional-remove-example[]
			Person person1 = entityManager.find( Person.class, personId );
			entityManager.remove( person1 );
			//end::associations-many-to-many-unidirectional-remove-example[]
		} );
	}

	//tag::associations-many-to-many-unidirectional-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
		private List<Address> addresses = new ArrayList<>();

		//Getters and setters are omitted for brevity

		//end::associations-many-to-many-unidirectional-example[]

		public Person() {
		}

		public List<Address> getAddresses() {
			return addresses;
		}
		//tag::associations-many-to-many-unidirectional-example[]
	}

	@Entity(name = "Address")
	public static class Address {

		@Id
		@GeneratedValue
		private Long id;

		private String street;

		@Column(name = "`number`")
		private String number;

		//Getters and setters are omitted for brevity

		//end::associations-many-to-many-unidirectional-example[]

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
		//tag::associations-many-to-many-unidirectional-example[]
	}
	//end::associations-many-to-many-unidirectional-example[]
}
