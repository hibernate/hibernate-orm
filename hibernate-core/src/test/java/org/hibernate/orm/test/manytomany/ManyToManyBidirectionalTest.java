/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomany;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.annotations.NaturalId;

import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ImplicitListAsBagProvider;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
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
				provider = ImplicitListAsBagProvider.class
		)
)
public class ManyToManyBidirectionalTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testRemoveOwnerSide(EntityManagerFactoryScope scope) {
		Person _person1 = scope.fromTransaction( entityManager -> {
			Person person1 = new Person( "ABC-123" );
			Person person2 = new Person( "DEF-456" );

			Address address1 = new Address( "12th Avenue", "12A", "4005A" );
			Address address2 = new Address( "18th Avenue", "18B", "4007B" );

			person1.addAddress( address1 );
			person1.addAddress( address2 );

			person2.addAddress( address1 );

			entityManager.persist( person1 );
			entityManager.persist( person2 );

			return person1;
		} );

		scope.inTransaction( entityManager -> {
			Person person1 = entityManager.find( Person.class, _person1.id );

			entityManager.remove( person1 );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB do not support FK violation checking")
	@FailureExpected(jiraKey = "HHH-12239")
	public void testRemoveMappedBySide(EntityManagerFactoryScope scope) {
		Address _address1 = scope.fromTransaction( entityManager -> {
			Person person1 = new Person( "ABC-123" );
			Person person2 = new Person( "DEF-456" );

			Address address1 = new Address( "12th Avenue", "12A", "4005A" );
			Address address2 = new Address( "18th Avenue", "18B", "4007B" );

			person1.addAddress( address1 );
			person1.addAddress( address2 );

			person2.addAddress( address1 );

			entityManager.persist( person1 );
			entityManager.persist( person2 );

			return address1;
		} );

		scope.inTransaction( entityManager -> {
			Address address1 = entityManager.find( Address.class, _address1.id );

			entityManager.remove( address1 );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String registrationNumber;

		@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
		private List<Address> addresses = new ArrayList<>();

		public Person() {
		}

		public Person(String registrationNumber) {
			this.registrationNumber = registrationNumber;
		}

		public void addAddress(Address address) {
			addresses.add( address );
			address.owners.add( this );
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

		public Address() {
		}

		public Address(String street, String number, String postalCode) {
			this.street = street;
			this.number = number;
			this.postalCode = postalCode;
		}
	}
}
