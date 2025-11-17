/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;

import org.hibernate.testing.orm.junit.SessionFactory;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-17947")
@BytecodeEnhanced
@EnhancementOptions(biDirectionalAssociationManagement = true)
@DomainModel(annotatedClasses = {
		AutoFlushBeforeLoadTest.Person.class,
		AutoFlushBeforeLoadTest.Phone.class,
		AutoFlushBeforeLoadTest.DriversLicense.class
})
@SessionFactory
public class AutoFlushBeforeLoadTest {
	@BeforeEach
	public void setupData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			Person p = new Person( 1L, "John Doe" );
			session.persist( p );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testCollectionInitialization(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			Phone phone = new Phone( session.getReference( Person.class, 1L ), "1234567890" );
			session.persist( phone );
			Person person = session.createQuery( "select p from Person p", Person.class ).getSingleResult();
			assertEquals( 1, person.getPhones().size() );
		} );
	}

	@Test
	public void testOneToOneInitialization(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			DriversLicense driversLicense = new DriversLicense( 1L, session.getReference( Person.class, 1L ), "999" );
			session.persist( driversLicense );
			Person person = session.createQuery( "select p from Person p", Person.class ).getSingleResult();
			assertEquals( driversLicense, person.getDriversLicense() );
			session.clear();
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@NaturalId
		private String name;

		@OneToOne(mappedBy = "owner", fetch = FetchType.LAZY)
		private DriversLicense driversLicense;

		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
		private List<Phone> phones = new ArrayList<>();

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Phone> getPhones() {
			return phones;
		}

		public DriversLicense getDriversLicense() {
			return driversLicense;
		}

		public void setDriversLicense(DriversLicense driversLicense) {
			this.driversLicense = driversLicense;
		}
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Person person;

		private String phoneNumber;

		public Phone() {
		}

		public Phone(Person person, String phoneNumber) {
			this.person = person;
			this.phoneNumber = phoneNumber;
		}

		public Long getId() {
			return id;
		}

		public String getPhoneNumber() {
			return phoneNumber;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}


	@Entity(name = "DriversLicense")
	public static class DriversLicense {

		@Id
		private Long id;
		@OneToOne
		@JoinColumn(name = "owner_name", referencedColumnName = "name")
		private Person owner;
		@NaturalId
		@Column(name = "license_number")
		private String number;

		public DriversLicense() {
		}

		public DriversLicense(Long id, Person owner, String number) {
			this.id = id;
			this.owner = owner;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Person getOwner() {
			return owner;
		}

		public void setOwner(Person owner) {
			this.owner = owner;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}
	}

}
