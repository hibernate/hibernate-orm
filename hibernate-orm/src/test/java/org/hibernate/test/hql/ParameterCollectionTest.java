/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10893")
public class ParameterCollectionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class};
	}

	@Override
	protected void prepareTest() throws Exception {
		try (Session session = openSession()) {
			session.beginTransaction();
			try {
				for ( int i = 0; i < 20; i++ ) {
					Person p1 = new Person( i, "p" + i );
					session.save( p1 );
				}
				session.getTransaction().commit();
			}
			catch (Exception e) {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Test
	public void testReusingQueryWithNewParameterValues() throws Exception {
		try (Session session = openSession()) {
			Collection<Long> ids = new ArrayList<>();
			Query q = session.createQuery( "select id from Person where id in (:ids) order by id" );
			for ( int i = 0; i < 10; i++ ) {
				ids.add( Long.valueOf( i ) );
			}
			q.setParameterList( "ids", ids );
			q.list();

			ids.clear();
			for ( int i = 10; i < 20; i++ ) {
				ids.add( Long.valueOf( i ) );
			}
			// reuse the same query, but set new collection parameter
			q.setParameterList( "ids", ids );
			List<Long> foundIds = q.list();

			assertThat( "Wrong number of results", foundIds.size(), is( ids.size() ) );
			assertThat( foundIds, is( ids ) );
		}
	}

	@Entity(name = "Person")
	@Table(name = "PERSON")
	public static class Person {
		@Id
		private long id;
		private String name;

		public Person() {
		}

		public Person(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
