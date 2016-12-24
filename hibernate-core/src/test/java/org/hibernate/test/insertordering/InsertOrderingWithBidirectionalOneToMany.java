/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-9864")
public class InsertOrderingWithBidirectionalOneToMany
		extends BaseNonConfigCoreFunctionalTestCase {
	private InsertOrderingStatementInspector statementInspector = new InsertOrderingStatementInspector();

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Address.class, Person.class };
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ORDER_INSERTS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "10" );
		settings.put( Environment.GENERATE_STATISTICS, "true" );
		settings.put( Environment.STATEMENT_INSPECTOR, statementInspector );
	}

	@Test
	public void testBatching() throws SQLException {
		sessionFactory().getStatistics().clear();
		statementInspector.clear();

		Session session = openSession();
		session.getTransaction().begin();

			Person father = new Person();
			Person mother = new Person();
			Person son = new Person();
			Person daughter = new Person();

			Address home = new Address();
			Address office = new Address();

			home.addPerson( father );
			home.addPerson( mother );
			home.addPerson( son );
			home.addPerson( daughter );

			office.addPerson( father );
			office.addPerson( mother );

			session.persist( home );
			session.persist( office );

		session.getTransaction().commit();
		session.close();

		assertEquals( 1, statementInspector.getCount( "insert into Address (ID) values (?)" ) );
		assertEquals( 2, sessionFactory().getStatistics().getEntityStatistics( Address.class.getName() ).getInsertCount() );

		assertEquals( 1, statementInspector.getCount( "insert into Person (address_ID, ID) values (?, ?)" ) );
		assertEquals( 4, sessionFactory().getStatistics().getEntityStatistics( Person.class.getName() ).getInsertCount() );
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		@OneToMany(mappedBy = "address", cascade = CascadeType.PERSIST)
		private List<Person> persons = new ArrayList<Person>();

		public void addPerson(Person person) {
			persons.add( person );
			person.address = this;
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@Column(name = "ID", nullable = false)
		@SequenceGenerator(name = "ID", sequenceName = "ADDRESS_SEQ")
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID")
		private Long id;

		@ManyToOne
		private Address address;
	}
}
