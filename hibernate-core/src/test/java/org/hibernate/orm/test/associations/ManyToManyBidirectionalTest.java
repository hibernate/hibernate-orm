/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.annotations.NaturalId;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				ManyToManyBidirectionalTest.Person.class,
				ManyToManyBidirectionalTest.Address.class,
		},
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ManyToManyBidirectionalTest.CollectionClassificationProvider.class
		)
)
public class ManyToManyBidirectionalTest {

	public static class CollectionClassificationProvider implements SettingProvider.Provider<CollectionClassification> {
		@Override
		public CollectionClassification getSetting() {
			return CollectionClassification.BAG;
		}
	}

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::associations-many-to-many-bidirectional-lifecycle-example[]
			Person person1 = new Person("ABC-123");
			Person person2 = new Person("DEF-456");

			Address address1 = new Address("12th Avenue", "12A", "4005A");
			Address address2 = new Address("18th Avenue", "18B", "4007B");

			person1.addAddress(address1);
			person1.addAddress(address2);

			person2.addAddress(address1);

			entityManager.persist(person1);
			entityManager.persist(person2);

			entityManager.flush();

			person1.removeAddress(address1);
			//end::associations-many-to-many-bidirectional-lifecycle-example[]
		});
	}

	//tag::associations-many-to-many-bidirectional-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String registrationNumber;

		@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
		private List<Address> addresses = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::associations-many-to-many-bidirectional-example[]

		public Person() {
		}

		public Person(String registrationNumber) {
			this.registrationNumber = registrationNumber;
		}

		public List<Address> getAddresses() {
			return addresses;
		}

	//tag::associations-many-to-many-bidirectional-example[]
		public void addAddress(Address address) {
			addresses.add(address);
			address.getOwners().add(this);
		}

		public void removeAddress(Address address) {
			addresses.remove(address);
			address.getOwners().remove(this);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Person person = (Person) o;
			return Objects.equals(registrationNumber, person.registrationNumber);
		}

		@Override
		public int hashCode() {
			return Objects.hash(registrationNumber);
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

		@ManyToMany(mappedBy = "addresses")
		private List<Person> owners = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::associations-many-to-many-bidirectional-example[]

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

		public List<Person> getOwners() {
			return owners;
		}

	//tag::associations-many-to-many-bidirectional-example[]
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Address address = (Address) o;
			return Objects.equals(street, address.street) &&
					Objects.equals(number, address.number) &&
					Objects.equals(postalCode, address.postalCode);
		}

		@Override
		public int hashCode() {
			return Objects.hash(street, number, postalCode);
		}
	}
	//end::associations-many-to-many-bidirectional-example[]
}
