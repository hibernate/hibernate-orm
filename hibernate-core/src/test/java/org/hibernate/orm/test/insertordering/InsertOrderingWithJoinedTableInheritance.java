/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.insertordering;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;

import org.hibernate.testing.TestForIssue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-9864")
public class InsertOrderingWithJoinedTableInheritance extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Address.class, Person.class, SpecialPerson.class };
	}

	@AfterEach
	public void tearDown() {
		sessionFactoryScope().inTransaction( session -> {
			session.createQuery( "delete from Address" ).executeUpdate();
			session.createQuery( "delete from Person" ).executeUpdate();
			session.createQuery( "delete from SpecialPerson" ).executeUpdate();
		} );
	}

	@Test
	public void testBatchOrdering() {
		sessionFactoryScope().inTransaction( session -> {
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

		verifyPreparedStatementCount( 26 );
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
		@SequenceGenerator(name = "ID_2", sequenceName = "PERSON_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_2")
		private Long id;

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
