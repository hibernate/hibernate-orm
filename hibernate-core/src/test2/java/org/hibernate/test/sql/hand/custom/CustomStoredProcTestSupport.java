/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.hand.custom;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.query.ParameterRecognitionException;
import org.hibernate.procedure.ProcedureCall;

import org.hibernate.test.sql.hand.Employment;
import org.hibernate.test.sql.hand.Organization;
import org.hibernate.test.sql.hand.Person;

import com.arjuna.ats.internal.jdbc.drivers.modifiers.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Abstract test case defining tests of stored procedure support.
 *
 * @author Gail Badner
 */
@SuppressWarnings( {"UnusedDeclaration"})
public abstract class CustomStoredProcTestSupport extends CustomSQLTestSupport {
	@Test
	public void testScalarStoredProcedure() throws HibernateException, SQLException {
		Session s = openSession();
		Query namedQuery = s.getNamedQuery( "simpleScalar" );
		namedQuery.setLong( "number", 43 );
		List list = namedQuery.list();
		Object o[] = ( Object[] ) list.get( 0 );
		assertEquals( o[0], "getAll" );
		assertEquals( o[1], Long.valueOf( 43 ) );
		s.close();
	}

	@Test
	public void testParameterHandling() throws HibernateException, SQLException {
		Session s = openSession();

		Query namedQuery = s.getNamedQuery( "paramhandling" );
		namedQuery.setLong( 1, 10 );
		namedQuery.setLong( 2, 20 );
		List list = namedQuery.list();
		Object[] o = ( Object[] ) list.get( 0 );
		assertEquals( o[0], Long.valueOf( 10 ) );
		assertEquals( o[1], Long.valueOf( 20 ) );
		s.close();
	}

	@Test
	public void testMixedParameterHandling() throws HibernateException, SQLException {
		inTransaction(
				session -> {
					try {
						session.getNamedQuery( "paramhandling_mixed" );
						fail( "Expecting an exception" );
					}
					catch (ParameterRecognitionException expected) {
						// expected outcome
					}
					catch (Exception other) {
						throw new AssertionError( "Expecting a ParameterRecognitionException, but encountered " + other.getClass().getSimpleName(), other );
					}
				}
		);
	}

	@Test
	public void testEntityStoredProcedure() throws HibernateException, SQLException {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Organization ifa = new Organization( "IFA" );
		Organization jboss = new Organization( "JBoss" );
		Person gavin = new Person( "Gavin" );
		Employment emp = new Employment( gavin, jboss, "AU" );
		s.persist( ifa );
		s.persist( jboss );
		s.persist( gavin );
		s.persist( emp );

		Query namedQuery = s.getNamedQuery( "selectAllEmployments" );
		List list = namedQuery.list();
		assertTrue( list.get( 0 ) instanceof Employment );
		s.delete( emp );
		s.delete( ifa );
		s.delete( jboss );
		s.delete( gavin );

		t.commit();
		s.close();
	}
}
