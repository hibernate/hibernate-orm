/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Query;
import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-12729")
public class HibernateFirstResultMaxResultsTest extends BaseNonConfigCoreFunctionalTestCase {

	protected Class[] getAnnotatedClasses() {
		return new Class[] { Employee.class };
	}

	@Test
	public void testFirstResult() {
		doInHibernate(
				this::sessionFactory,
				session -> {

					Query query = session.createQuery( "from Employee" );

					// not initialized yet
					assertNull( query.getHibernateFirstResult() );

					// the following is special case; when initialized to -1, getHibernateFirstResult returns 0
					assertEquals( Integer.valueOf( 0 ), query.setHibernateFirstResult( -1 ).getHibernateFirstResult() );

					assertEquals( Integer.valueOf( 0 ), query.setHibernateFirstResult( 0 ).getHibernateFirstResult() );
					assertEquals( Integer.valueOf( 1 ), query.setHibernateFirstResult( 1 ).getHibernateFirstResult() );

					assertEquals( Integer.valueOf( 10 ), query.setFirstResult( 10 ).getHibernateFirstResult() );
				}
		);
	}

	@Test
	public void testMaxResults() {
		doInHibernate(
				this::sessionFactory,
				session -> {

					Query query = session.createQuery( "from Employee" );

					// not initialized yet
					assertNull( query.getHibernateMaxResults() );

					// values <= 0 are considered uninitialized;
					assertNull( query.setHibernateMaxResults( -1 ).getHibernateMaxResults() );
					assertNull( query.setHibernateMaxResults( 0 ).getHibernateMaxResults() );

					assertEquals( Integer.valueOf( 1 ), query.setHibernateMaxResults( 1 ).getHibernateMaxResults() );

					assertEquals( Integer.valueOf( 0 ), query.setMaxResults( 0 ).getHibernateMaxResults() );

					assertEquals( Integer.valueOf( 2 ), query.setMaxResults( 2 ).getHibernateMaxResults() );
				}
		);
	}

	@Test
	public void testPagination() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					for ( int i = 0; i < 5; i++ ) {
						session.persist( new Employee( i ) );
					}
				}
		);

		final String query = "from Employee order by id";
		checkResults( executeQuery( query, null, null ), 0, 4 );
		checkResults( executeQuery( query, 0, null ), 0, 4 );
		checkResults( executeQuery( query, -1, null ), 0, 4 );
		checkResults( executeQuery( query, null, 0 ), 0, 4 );
		checkResults( executeQuery( query, null, -1 ), 0, 4 );
		checkResults( executeQuery( query, null, 2 ), 0, 1 );
		checkResults( executeQuery( query, -1, 0 ), 0, 4 );
		checkResults( executeQuery( query, -1, 3 ), 0, 2 );
		checkResults( executeQuery( query, 1, null ), 1, 4 );
		checkResults( executeQuery( query, 1, 0 ), 1, 4 );
		checkResults( executeQuery( query, 1, -1 ), 1, 4 );
		checkResults( executeQuery( query, 1, 1 ), 1, 1 );
	}

	public List executeQuery(String queryString, Integer firstResult, Integer maxResults) {
		return doInHibernate(
				this::sessionFactory,
				session -> {
					Query query = session.createQuery( queryString );
					if ( firstResult != null ) {
						query.setHibernateFirstResult( firstResult );
					}
					if ( maxResults != null ) {
						query.setHibernateMaxResults( maxResults );
					}
					return query.list();
				}
		);
	}

	private void checkResults( List results, int firstIdExpected, int lastIdExpected ) {
		int resultIndex = 0;
		for( int i = firstIdExpected ; i <= lastIdExpected ; i++, resultIndex++ ) {
			assertEquals( i, ( (Employee) results.get( resultIndex ) ).id );
		}
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		private long id;

		private String name;

		public Employee() {
		}

		public Employee(long id) {
			this.id = id;
		}
	}

}
