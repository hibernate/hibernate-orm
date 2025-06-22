/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;


/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-9864")
public class InsertOrderingWithTablePerClassInheritance extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Address.class, Person.class, SpecialPerson.class };
	}

	@AfterEach
	public void tearDown() {
		sessionFactoryScope().getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testBatchOrdering() {
		clearBatches();
		sessionFactoryScope().inTransaction( session -> {
			// First object with dependent object (address)
			final Person person = new Person( 1, "Baboo" );
			person.addAddress( new Address( 1, "123 Main St" ) );
			session.persist( person );

			// Derived Object with dependent object (address)
			final SpecialPerson specialPerson = new SpecialPerson( 2, "Bamboozler", "stuff" );
			specialPerson.addAddress( new Address( 2, "123 1st St" ) );
			session.persist( specialPerson );
		} );
	}

	@Test
	public void testBatchingAmongstSubClasses() {
		clearBatches();
		sessionFactoryScope().inTransaction( session -> {
			int iterations = 12;
			for ( int i = 0; i/2 < iterations; i+=2 ) {
				final Person person = new Person( i );
				person.addAddress( new Address( i ) );
				session.persist( person );

				final SpecialPerson specialPerson = new SpecialPerson( i + 1);
				specialPerson.addAddress( new Address( i + 1 ) );
				session.persist( specialPerson );
			}
		} );

		// 1 for first 10 Person (1)
		// 0 for final 2 Person (reused)
		// 1 for first 10 SpecialPerson (2)
		// 0 for last 2 SpecialPerson (reused)
		// 1 for first 10 Address (3)
		// 0 for second 10 Address (reused)
		// 0 for final 4 Address (reused)
		verifyPreparedStatementCount( 3 );
	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS")
	@Access(AccessType.FIELD)
	public static class Address {
		@Id
		@Column(name = "ID", nullable = false)
		private Integer id;

		private String street;

		public Address() {
		}

		public Address(Integer id) {
			this.id = id;
		}

		public Address(Integer id, String street) {
			this.id = id;
			this.street = street;
		}

		public Integer getId() {
			return id;
		}

		protected void setId(Integer id) {
			this.id = id;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}
	}

	@Entity(name = "Person")
	@Access(AccessType.FIELD)
	@Table(name = "PERSON")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@DiscriminatorColumn(name = "CLASSINDICATOR", discriminatorType = DiscriminatorType.INTEGER)
	@DiscriminatorValue("1")
	public static class Person {
		@Id
		@Column(name = "ID", nullable = false)
		private Integer id;

		private String name;

		@OneToMany(orphanRemoval = true, cascade = {
				CascadeType.PERSIST,
				CascadeType.REMOVE
		})
		@JoinColumn(name = "PERSONID", referencedColumnName = "ID", nullable = false, updatable = false)
		@BatchSize(size = 100)
		private Set<Address> addresses = new HashSet<>();

		protected Person() {
		}

		public Person(Integer id) {
			this.id = id;
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void addAddress(Address address) {
			this.addresses.add( address );
		}
	}

	@Entity(name = "SpecialPerson")
	@Access(AccessType.FIELD)
	@DiscriminatorValue("2")
	public static class SpecialPerson extends Person {
		@Column(name = "special")
		private String special;

		protected SpecialPerson() {
			super();
		}

		public SpecialPerson(Integer id) {
			super( id );
		}

		public SpecialPerson(Integer id, String name, String special) {
			super( id, name );
			this.special = special;
		}

		public String getSpecial() {
			return special;
		}

		public void setSpecial(String special) {
			this.special = special;
		}
	}
}
