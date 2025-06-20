/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-9864")
public class InsertOrderingWithJoinedTableMultiLevelInheritance extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Address.class,
				Person.class,
				SpecialPerson.class,
				AnotherPerson.class,
				President.class,
				Office.class
		};
	}

	@Test
	public void testBatchingAmongstSubClasses() {
		sessionFactoryScope().inTransaction( session -> {
			int iterations = 2;
			for ( int i = 0; i < iterations; i++ ) {
				final President president = new President();
				president.addAddress( new Address() );
				session.persist( president );

				final AnotherPerson anotherPerson = new AnotherPerson();
				Office office = new Office();
				session.persist( office );
				anotherPerson.office = office;
				session.persist( anotherPerson );

				final Person person = new Person();
				session.persist( person );

				final SpecialPerson specialPerson = new SpecialPerson();
				specialPerson.addAddress( new Address() );
				session.persist( specialPerson );
			}
			clearBatches();
		} );

		// 1 for Person (1)
		// 2 for SpecialPerson (3)
		// 2 for AnotherPerson (5)
		// 3 for President (8)
		// 1 for Address (9)
		// 1 for Office (10)
		verifyPreparedStatementCount( 10 );

		sessionFactoryScope().inTransaction( (session) -> {
			// 2 Address per loop (4)
			// 1 Office per loop (2)
			// 1 Person per loop (2)
			// 1 SpecialPerson per loop (2)
			// 1 AnotherPerson per loop (2)
			// 1 President per loop (2)
			final Long addressCount = session
					.createSelectionQuery( "select count(1) from Address", Long.class )
					.getSingleResult();
			assertThat( addressCount ).isEqualTo( 4L );

			final Long officeCount = session
					.createSelectionQuery( "select count(1) from Office", Long.class )
					.getSingleResult();
			assertThat( officeCount ).isEqualTo( 2L );

			final Long presidentCount = session
					.createSelectionQuery( "select count(1) from President", Long.class )
					.getSingleResult();
			assertThat( presidentCount ).isEqualTo( 2L );

			final Long anotherPersonCount = session
					.createSelectionQuery( "select count(1) from AnotherPerson", Long.class )
					.getSingleResult();
			assertThat( presidentCount ).isEqualTo( 2L );

		} );


	}

	@After
	protected void cleanupTestData() {
		sessionFactoryScope().getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS")
	public static class Address {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;
	}

	@Entity(name = "Office")
	public static class Office {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID_2", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_2")
		private Long id;
	}

	@Entity(name = "Person")
	@Table(name = "PERSON")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "CLASSINDICATOR", discriminatorType = DiscriminatorType.INTEGER)
	public static class Person {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID_3", sequenceName = "PERSON_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_3")
		private Long id;

	}

	@Entity(name = "SpecialPerson")
	public static class SpecialPerson extends Person {
		@Column(name = "special")
		private String special;

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

	@Entity(name = "AnotherPerson")
	public static class AnotherPerson extends Person {
		private boolean working;

		@ManyToOne
		private Office office;
	}

	@Entity(name = "President")
	public static class President extends SpecialPerson {

		@Column(name = "salary")
		private BigDecimal salary;
	}
}
