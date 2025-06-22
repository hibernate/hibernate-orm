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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-9864")
public class InsertOrderingWithSingleTableInheritance extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Address.class, Person.class, SpecialPerson.class };
	}

	@AfterEach
	public void tearDown() {
		sessionFactoryScope().getSessionFactory().getSchemaManager().truncate();;
	}

	@Test
	public void testBatchOrdering() {
		sessionFactoryScope().inTransaction( session -> {
			// First object with dependent object (address)
			final Person person = new Person();
			person.addAddress( new Address() );
			session.persist( person );

			// Derived Object with dependent object (address)
			final SpecialPerson specialPerson = new SpecialPerson();
			specialPerson.addAddress( new Address() );
			session.persist( specialPerson );
		} );
	}

	@Test
	public void testBatchingAmongstSubClasses() {
		sessionFactoryScope().inTransaction( session -> {
			int iterations = 12;
			for ( int i = 0; i < iterations; i++ ) {
				final Person person = new Person();
				person.addAddress( new Address() );
				session.persist( person );

				final SpecialPerson specialPerson = new SpecialPerson();
				specialPerson.addAddress( new Address() );
				session.persist( specialPerson );
			}
			clearBatches();
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
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		private String street;
	}

	@Entity(name = "Person")
	@Access(AccessType.FIELD)
	@Table(name = "PERSON")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "CLASSINDICATOR", discriminatorType = DiscriminatorType.INTEGER)
	@DiscriminatorValue("1")
	public static class Person {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID_2", sequenceName = "PERSON_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_2")
		private Long id;

		private String name;

		@OneToMany(orphanRemoval = true, cascade = {
				CascadeType.PERSIST,
				CascadeType.REMOVE
		})
		@JoinColumn(name = "PERSONID", referencedColumnName = "ID", nullable = false, updatable = false)
		@BatchSize(size = 100)
		private Set<Address> addresses = new HashSet<>();

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
	}
}
