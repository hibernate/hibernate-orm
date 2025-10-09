/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;

import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DomainModel(
		annotatedClasses = {
				PrivateConstructorEnhancerTest.Person.class,
				PrivateConstructorEnhancerTest.Country.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class PrivateConstructorEnhancerTest {

	private Country country;
	private Person person;

	@BeforeEach
	protected void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			country = new Country( "Romania" );
			person = new Person( "Vlad Mihalcea", country );
			session.persist( country );
			session.persist( person );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testFindEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				Country country = session.find( Country.class, this.country.id );

				assertNotNull( country.getName(), "Romania" );
				fail( "Should have thrown exception" );
			}
			catch (Exception expected) {
				assertTrue( expected.getMessage().contains( "No default constructor for entity" ) );
			}
		} );
	}

	@Test
	public void testGetReferenceEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				Country country = session.getReference( Country.class, this.country.id );

				assertNotNull( country.getName(), "Romania" );
				fail( "Should have thrown exception" );
			}
			catch (Exception expected) {
				assertTrue( expected.getMessage().contains( "No default constructor for entity" ) );
			}
		} );
	}

	@Test
	public void testLoadProxyAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				Person person = session.find( Person.class, this.person.id );

				assertNotNull( person.getCountry().getName(), "Romania" );
				fail( "Should have thrown exception" );
			}
			catch (Exception expected) {
				assertTrue( expected.getMessage().contains( "No default constructor for entity" ) );
			}
		} );
	}

	@Test
	public void testListEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				List<Person> persons = session.createQuery( "select p from Person p", Person.class ).getResultList();
				assertTrue( persons.stream().anyMatch( p -> p.getCountry().getName().equals( "Romania" ) ) );

				fail( "Should have thrown exception" );
			}
			catch (Exception expected) {
				assertTrue( expected.getMessage().contains( "No default constructor for entity" ) );
			}
		} );
	}

	@Test
	public void testListJoinFetchEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				List<Person> persons = session.createQuery( "select p from Person p join fetch p.country", Person.class )
						.getResultList();
				assertTrue( persons.stream().anyMatch( p -> p.getCountry().getName().equals( "Romania" ) ) );

				fail( "Should have thrown exception" );
			}
			catch (Exception expected) {
				assertTrue( expected.getMessage().contains( "No default constructor for entity" ) );
			}
		} );
	}

	@Entity(name = "Person")
	static class Person {

		@Id
		@GeneratedValue
		private int id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Country country;

		public Person() {
		}

		private Person(String name, Country country) {
			this.name = name;
			this.country = country;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Country getCountry() {
			return country;
		}
	}

	@Entity(name = "Country")
	static class Country {

		@Id
		@GeneratedValue
		private int id;

		@Basic(fetch = FetchType.LAZY)
		private String name;

		private Country(String name) {
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
