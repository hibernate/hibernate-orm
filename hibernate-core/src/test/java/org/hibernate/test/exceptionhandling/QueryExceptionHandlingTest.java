/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.exceptionhandling;

import static org.junit.Assert.fail;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.junit.Before;
import org.junit.Test;

@RequiresDialect(H2Dialect.class)
public class QueryExceptionHandlingTest extends BaseExceptionHandlingTest {

	public QueryExceptionHandlingTest(
			BootstrapMethod bootstrapMethod,
			ExceptionHandlingSetting exceptionHandlingSetting,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod, exceptionHandlingSetting, exceptionExpectations );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class
		};
	}

	@Before
	public void initData() {
		TransactionUtil2.inTransaction( sessionFactory(), s -> {
			s.createQuery( "delete from A" ).executeUpdate();
			A a1 = new A();
			a1.id = 1;
			s.persist( a1 );
			A a2 = new A();
			a2.id = 2;
			s.persist( a2 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12666")
	public void testInvalidQuery() {
		try {
			TransactionUtil2.inSession( sessionFactory(), s -> {
				s.createQuery( "from A where blahblahblah" ).list();
			} );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onInvalidQueryExecuted( expected );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13300")
	public void testGetSingleResultWithMultipleResults() {
		try {
			TransactionUtil2.inSession( sessionFactory(), s -> {
				s.createQuery( "from A where id in (1, 2)" ).getSingleResult();
			} );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onGetSingleResultWithMultipleResults( expected );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13300")
	public void testGetSingleResultWithNoResults() {
		try {
			TransactionUtil2.inSession( sessionFactory(), s -> {
				s.createQuery( "from A where id = 3" ).getSingleResult();
			} );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onGetSingleResultWithNoResults( expected );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13300")
	public void testExecuteUpdateWithConstraintViolation() {
		try {
			TransactionUtil2.inTransaction( sessionFactory(), s -> {
				s.createQuery( "update A set id = 1 where id = 2" ).executeUpdate();
			} );
			fail( "should have thrown an exception" );
		}
		catch (RuntimeException expected) {
			exceptionExpectations.onExecuteUpdateWithConstraintViolation( expected );
		}
	}

	@Entity(name = "A")
	public static class A {
		@Id
		private long id;
	}
}
