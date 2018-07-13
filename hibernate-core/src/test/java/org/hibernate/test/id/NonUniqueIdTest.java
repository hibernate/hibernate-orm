/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
public class NonUniqueIdTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Category.class };
	}

	@Before
	public void setup() {
		Session session = openSession();
		session.beginTransaction();
		{
					// drop and recreate table so it has no primary key

					session.createSQLQuery(
							"DROP TABLE CATEGORY"
					).executeUpdate();

					session.createSQLQuery(
							"create table CATEGORY( id integer not null, name varchar(255) )"
					).executeUpdate();

					session.createSQLQuery( "insert into CATEGORY( id, name) VALUES( 1, 'clothes' )" )
							.executeUpdate();
					session.createSQLQuery( "insert into CATEGORY( id, name) VALUES( 1, 'shoes' )" )
							.executeUpdate();

		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12802" )
	public void testLoadEntityWithNonUniqueId() {
		Session session = openSession();
		session.beginTransaction();

		try {
			session.get( Category.class, 1 );
			fail( "should have failed because there are 2 entities with id == 1" );
		}
		catch ( HibernateException ex) {
						// expected
		}
		finally {
			session.getTransaction().rollback();
			session.close();
		}
	}

	@Entity
	@Table(name = "CATEGORY")
	public static class Category {
		@Id
		private int id;

		private String name;
	}
}
