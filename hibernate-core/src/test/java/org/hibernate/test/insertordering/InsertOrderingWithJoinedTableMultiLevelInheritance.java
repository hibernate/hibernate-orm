/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import javax.persistence.ManyToOne;
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
public class InsertOrderingWithJoinedTableMultiLevelInheritance
		extends BaseNonConfigCoreFunctionalTestCase {
	private InsertOrderingStatementInspector statementInspector = new InsertOrderingStatementInspector();

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Address.class,
				Person.class,
				SpecialPerson.class,
				AnotherPerson.class,
				President.class,
				Office.class
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ORDER_INSERTS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "10" );
		settings.put( Environment.GENERATE_STATISTICS, "true" );
		settings.put( Environment.STATEMENT_INSPECTOR, statementInspector );
	}

	@Test
	public void testBatchingAmongstSubClasses() {
		sessionFactory().getStatistics().clear();
		statementInspector.clear();

		Session session = openSession();
		session.getTransaction().begin();

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

		session.getTransaction().commit();
		session.close();

		assertEquals( 1, statementInspector.getCount( "insert into ADDRESS (PERSONID, ID) values (?, ?)" ) );
		assertEquals(
				4,
				sessionFactory().getStatistics().getEntityStatistics( Address.class.getName() ).getInsertCount()
		);

		assertEquals( 1, statementInspector.getCount( "insert into Office (ID) values (?)" ) );
		assertEquals( 2, sessionFactory().getStatistics().getEntityStatistics( Office.class.getName() ).getInsertCount() );

		assertEquals( 1, statementInspector.getCount( "insert into PERSON (CLASSINDICATOR, ID) values (1, ?)" ) );
		assertEquals( 2, sessionFactory().getStatistics().getEntityStatistics( Person.class.getName() ).getInsertCount() );

		assertEquals( 2, statementInspector.getCount( "insert into PERSON (CLASSINDICATOR, ID) values (2, ?)" ) );
		assertEquals( 4, statementInspector.getCount( "insert into SpecialPerson (special, ID) values (?, ?)" ) );
		assertEquals( 2, sessionFactory().getStatistics().getEntityStatistics( SpecialPerson.class.getName() ).getInsertCount() );

		assertEquals( 2, statementInspector.getCount( "insert into PERSON (CLASSINDICATOR, ID) values (3, ?)" ) );
		assertEquals( 2, statementInspector.getCount( "insert into AnotherPerson (office_ID, working, ID) values (?, ?, ?)" ) );
		assertEquals( 2, sessionFactory().getStatistics().getEntityStatistics( AnotherPerson.class.getName() ).getInsertCount() );

		assertEquals( 2, statementInspector.getCount( "insert into PERSON (CLASSINDICATOR, ID) values (4, ?)" ) );
		assertEquals( 2, statementInspector.getCount( "insert into President (salary, ID) values (?, ?)" ) );
		assertEquals( 2, sessionFactory().getStatistics().getEntityStatistics( President.class.getName() ).getInsertCount() );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected void cleanupTestData() throws Exception {
		Session session = openSession();
		session.getTransaction().begin();
		

		try {
			session.createQuery("delete Address").executeUpdate();
			session.createQuery("delete Person").executeUpdate();
			session.createQuery("delete SpecialPerson").executeUpdate();
			session.createQuery("delete AnotherPerson").executeUpdate();
			session.createQuery("delete Office").executeUpdate();
			session.createQuery("delete President").executeUpdate();

			session.getTransaction().commit();
		} finally {
			session.close();
		}
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
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;
	}

	@Entity(name = "Person")
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

	}

	@Entity(name = "SpecialPerson")
	@DiscriminatorValue("2")
	public static class SpecialPerson extends Person {
		@Column(name = "special")
		private String special;

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

	@Entity(name = "AnotherPerson")
	@DiscriminatorValue("3")
	public static class AnotherPerson extends Person {
		private boolean working;

		@ManyToOne
		private Office office;
	}

	@Entity(name = "President")
	@DiscriminatorValue("4")
	public static class President extends SpecialPerson {

		@Column(name = "salary")
		private BigDecimal salary;
	}
}
