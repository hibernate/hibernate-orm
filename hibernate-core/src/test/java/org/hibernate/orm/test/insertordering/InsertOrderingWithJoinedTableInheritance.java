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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-9864")
public class InsertOrderingWithJoinedTableInheritance extends BaseInsertOrderingTest {

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
		sessionFactoryScope().inTransaction( session -> {
			final Person person = new Person( 1 );
			person.addAddress( new Address( 1 ) );
			session.persist( person );

			// Derived Object with dependent object (address)
			final SpecialPerson specialPerson = new SpecialPerson( 2, "#" + 2 );
			specialPerson.addAddress( new Address( 2 ) );
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

				final SpecialPerson specialPerson = new SpecialPerson( i+1, "#" + i+1 );
				specialPerson.addAddress( new Address( i+1 ) );
				session.persist( specialPerson );
			}
		} );

		// 1 for first 10 Person
		// 0 for final 2 Person (reused)
		// 2 for first 10 SpecialPerson (3)
		// 0 for last 2 SpecialPerson (reused)
		// 1 for first 10 Address (4)
		// 0 for second 10 Address (reused)
		// 0 for final 4 Address (reused)
		verifyPreparedStatementCount( 4 );

		sessionFactoryScope().inTransaction( (session) -> {
			final Long specialPersonCount = session
					.createSelectionQuery( "select count(1) from SpecialPerson", Long.class )
					.getSingleResult();
			assertThat( specialPersonCount ).isEqualTo( 12L );

			final Long addressCount = session
					.createSelectionQuery( "select count(1) from Address", Long.class )
					.getSingleResult();
			assertThat( addressCount ).isEqualTo( 24L );
		} );

	}

	@AfterEach
	public void dropTestData() {
		sessionFactoryScope().getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS")
	@Access(AccessType.FIELD)
	public static class Address {
		@Id
		@Column(name = "ID", nullable = false)
		private Integer id;

		protected Address() {
		}

		public Address(int id) {
			this.id = id;
		}
	}

	@Entity(name = "Person")
	@Access(AccessType.FIELD)
	@Table(name = "PERSON")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "CLASSINDICATOR", discriminatorType = DiscriminatorType.INTEGER)
	@DiscriminatorValue("1")
	public static class Person {
		@Id
		@Column(name = "ID", nullable = false)
		private Integer id;

		@OneToMany(orphanRemoval = true, cascade = {
				CascadeType.PERSIST,
				CascadeType.REMOVE
		})
		@JoinColumn(name = "PERSONID", referencedColumnName = "ID", nullable = false, updatable = false)
		@BatchSize(size = 100)
		private Set<Address> addresses = new HashSet<>();

		protected Person() {
		}

		public Person(int id) {
			this.id = id;
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


		public SpecialPerson() {
		}

		public SpecialPerson(int id, String special) {
			super( id );
			this.special = special;
		}
	}
}
