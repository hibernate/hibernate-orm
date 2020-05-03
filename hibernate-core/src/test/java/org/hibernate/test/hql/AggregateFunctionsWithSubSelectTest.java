/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Tuple;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author bjoern.moritz
 */
@TestForIssue(jiraKey = "HHH-9331")
@RequiresDialect(H2Dialect.class)
public class AggregateFunctionsWithSubSelectTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Document.class,
			Person.class
		};
	}

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			Document document = new Document();
			document.setId( 1 );

			Person p1 = new Person();
			Person p2 = new Person();

			p1.getLocalized().put(1, "p1.1");
			p1.getLocalized().put(2, "p1.2");
			p2.getLocalized().put(1, "p2.1");
			p2.getLocalized().put(2, "p2.2");

			document.getContacts().put(1, p1);
			document.getContacts().put(2, p2);

			session.persist(p1);
			session.persist(p2);
			session.persist(document);
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testSum() {

		doInHibernate( this::sessionFactory, session -> {
			List results = session.createQuery(
				"SELECT " +
				"	d.id, " +
				"	SUM(" +
				"		(" +
				"			SELECT COUNT(localized) " +
				"			FROM Person p " +
				"			LEFT JOIN p.localized localized " +
				"			WHERE p.id = c.id" +
				"		)" +
				"	) AS localizedCount " +
				"FROM Document d " +
				"LEFT JOIN d.contacts c " +
				"GROUP BY d.id")
			.getResultList();

			assertEquals(1, results.size());
			Object[] tuple = (Object[]) results.get( 0 );
			assertEquals(1, tuple[0]);
		} );
	}

	@Test
	public void testMin() {
		doInHibernate( this::sessionFactory, session -> {
			List results = session.createQuery(
				"SELECT " +
				"	d.id, " +
				"	MIN(" +
				"		(" +
				"			SELECT COUNT(localized) " +
				"			FROM Person p " +
				"			LEFT JOIN p.localized localized " +
				"			WHERE p.id = c.id" +
				"		)" +
				"	) AS localizedCount " +
				"FROM Document d " +
				"LEFT JOIN d.contacts c " +
				"GROUP BY d.id")
			.getResultList();

			assertEquals(1, results.size());
			Object[] tuple = (Object[]) results.get( 0 );
			assertEquals(1, tuple[0]);
		} );
	}

	@Test
	public void testMax() {
		doInHibernate( this::sessionFactory, session -> {
			List results = session.createQuery(
				"SELECT " +
				"	d.id, " +
				"	MAX(" +
				"		(" +
				"			SELECT COUNT(localized) " +
				"			FROM Person p " +
				"			LEFT JOIN p.localized localized " +
				"			WHERE p.id = c.id" +
				"		)" +
				"	) AS localizedCount " +
				"FROM Document d " +
				"LEFT JOIN d.contacts c " +
				"GROUP BY d.id")
			.getResultList();

			assertEquals(1, results.size());
			Object[] tuple = (Object[]) results.get( 0 );
			assertEquals(1, tuple[0]);
		} );
	}

	@Test
	public void testAvg() {
		doInHibernate( this::sessionFactory, session -> {
			List results = session.createQuery(
				"SELECT " +
				"	d.id, " +
				"	AVG(" +
				"		(" +
				"			SELECT COUNT(localized) " +
				"			FROM Person p " +
				"			LEFT JOIN p.localized localized " +
				"			WHERE p.id = c.id" +
				"		)" +
				"	) AS localizedCount " +
				"FROM Document d " +
				"LEFT JOIN d.contacts c " +
				"GROUP BY d.id")
			.getResultList();

			assertEquals(1, results.size());
			Object[] tuple = (Object[]) results.get( 0 );
			assertEquals(1, tuple[0]);
		} );
	}

	@Entity(name = "Document")
	public static class Document {

		private Integer id;
		private Map<Integer, Person> contacts = new HashMap<>();

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@OneToMany
		@CollectionTable
		@MapKeyColumn(name = "position")
		public Map<Integer, Person> getContacts() {
			return contacts;
		}

		public void setContacts(Map<Integer, Person> contacts) {
			this.contacts = contacts;
		}
	}


	@Entity(name = "Person")
	public static class Person {

		private Integer id;

		private Map<Integer, String> localized = new HashMap<>();

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ElementCollection
		public Map<Integer, String> getLocalized() {
			return localized;
		}

		public void setLocalized(Map<Integer, String> localized) {
			this.localized = localized;
		}
	}

}
