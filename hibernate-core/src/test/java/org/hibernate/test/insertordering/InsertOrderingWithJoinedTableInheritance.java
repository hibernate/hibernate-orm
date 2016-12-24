/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.util.HashSet;
import java.util.Map;
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

import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-9864")
public class InsertOrderingWithJoinedTableInheritance
		extends BaseNonConfigCoreFunctionalTestCase {
	private InsertOrderingStatementInspector statementInspector = new InsertOrderingStatementInspector();

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Address.class, Person.class, SpecialPerson.class };
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ORDER_INSERTS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "10" );
		settings.put( Environment.GENERATE_STATISTICS, "true" );
		settings.put( Environment.STATEMENT_INSPECTOR, statementInspector );
	}

	@Test
	public void testBatchOrdering() {
		Session session = openSession();
		session.getTransaction().begin();

			final Person person = new Person();
			person.addAddress( new Address() );
			session.persist( person );

			// Derived Object with dependent object (address)
			final SpecialPerson specialPerson = new SpecialPerson();
			specialPerson.addAddress( new Address() );
			session.persist( specialPerson );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testBatchingAmongstSubClasses() {
		sessionFactory().getStatistics().clear();
		statementInspector.clear();

		Session session = openSession();
		session.getTransaction().begin();

			int iterations = 12;
			for ( int i = 0; i < iterations; i++ ) {
				final Person person = new Person();
				person.addAddress( new Address() );
				session.persist( person );

				final SpecialPerson specialPerson = new SpecialPerson();
				specialPerson.addAddress( new Address() );
				session.persist( specialPerson );
			}

		session.getTransaction().commit();
		session.close();

		assertEquals( 1, statementInspector.getCount( "insert into ADDRESS (PERSONID, ID) values (?, ?)" ) );
		assertEquals(
				24,
				sessionFactory().getStatistics().getEntityStatistics( Address.class.getName() ).getInsertCount()
		);

		assertEquals( 1, statementInspector.getCount( "insert into PERSON (CLASSINDICATOR, ID) values (1, ?)" ) );
		assertEquals( 12, sessionFactory().getStatistics().getEntityStatistics( Person.class.getName() ).getInsertCount() );

		assertEquals( 12, statementInspector.getCount( "insert into PERSON (CLASSINDICATOR, ID) values (2, ?)" ) );
		assertEquals( 12, statementInspector.getCount( "insert into SpecialPerson (special, ID) values (?, ?)" ) );
		assertEquals( 12, sessionFactory().getStatistics().getEntityStatistics( SpecialPerson.class.getName() ).getInsertCount() );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
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
		@SequenceGenerator(name = "ID", sequenceName = "PERSON_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		@OneToMany(orphanRemoval = true, cascade = {
				CascadeType.PERSIST,
				CascadeType.REMOVE
		})
		@JoinColumn(name = "PERSONID", referencedColumnName = "ID", nullable = false, updatable = false)
		@BatchSize(size = 100)
		private Set<Address> addresses = new HashSet<Address>();

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
