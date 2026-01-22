/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@BytecodeEnhanced
@SessionFactory
@DomainModel(annotatedClasses = { FancyDefaultConstructorEnhancementTest.Person.class,
		FancyDefaultConstructorEnhancementTest.Address.class,
		FancyDefaultConstructorEnhancementTest.BaseEntity.class })
public class FancyDefaultConstructorEnhancementTest {

	@Test
	public void testDefaultConstructorAdded() {
		// Check if the default constructor exists for Person
		try {
			Assertions.assertNotNull( Person.class.getConstructor() );
		}
		catch (NoSuchMethodException e) {
			Assertions.fail( "Default constructor should have been added to Person by bytecode enhancement" );
		}

		// Check if the default constructor exists for Address
		try {
			Assertions.assertNotNull( Address.class.getConstructor() );
		}
		catch (NoSuchMethodException e) {
			Assertions.fail( "Default constructor should have been added to Address by bytecode enhancement" );
		}

		// Check if the default constructor exists for BaseEntity
		try {
			Assertions.assertNotNull( BaseEntity.class.getConstructor() );
		}
		catch (NoSuchMethodException e) {
			Assertions.fail( "Default constructor should have been added to BaseEntity by bytecode enhancement" );
		}
	}

	@Test
	public void testPersistAndLoad(SessionFactoryScope scope) {
		Person person = new Person( "Gavin", "Main St", "12345" );
		person.id = 1L;
		person.version = 1;

		scope.inTransaction( session -> {
			session.persist( person );
		} );

		scope.inTransaction( session -> {
			Person loadedPerson = session.find( Person.class, 1L );
			Assertions.assertNotNull( loadedPerson );
			Assertions.assertEquals( "Gavin", loadedPerson.name );
			Assertions.assertNotNull( loadedPerson.address );
			Assertions.assertEquals( "Main St", loadedPerson.address.street );
			Assertions.assertEquals( "12345", loadedPerson.address.zip );
			Assertions.assertEquals( 1, loadedPerson.version );
		} );
	}

	@MappedSuperclass
	public static class BaseEntity {
		@Version
		int version;

		// No default constructor
		public BaseEntity(int version) {
			this.version = version;
		}
	}

	@Entity(name = "Person")
	public static class Person extends BaseEntity {
		@Id
		Long id;

		final String name;

		@Embedded
		final Address address;

		// No default constructor
		public Person(String name, String street, String zip) {
			super(0);
			this.name = name;
			this.address = new Address(street, zip);
		}
	}

	@Embeddable
	public static class Address {
		final String street;
		final String zip;

		// No default constructor
		public Address(String street, String zip) {
			this.street = street;
			this.zip = zip;
		}
	}
}
