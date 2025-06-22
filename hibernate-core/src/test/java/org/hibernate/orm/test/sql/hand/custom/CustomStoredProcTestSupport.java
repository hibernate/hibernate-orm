/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.custom;

import java.sql.SQLException;
import java.util.List;

import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.orm.test.sql.hand.Employment;
import org.hibernate.orm.test.sql.hand.Organization;
import org.hibernate.orm.test.sql.hand.Person;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Abstract test case defining tests of stored procedure support.
 *
 * @author Gail Badner
 */
@SuppressWarnings("unused")
public abstract class CustomStoredProcTestSupport extends CustomSQLTestSupport {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Test
	public void testScalarStoredProcedure() throws HibernateException, SQLException {
		Session s = openSession();
//		Query namedQuery = s.getNamedQuery( "simpleScalar" );
		ProcedureCall namedQuery = s.createNamedStoredProcedureQuery( "simpleScalar" );
		namedQuery.setParameter( "p_number", 43 );
		List list = namedQuery.getResultList();
		Object o[] = ( Object[] ) list.get( 0 );
		assertEquals( o[0], "getAll" );
		assertEquals( o[1], Long.valueOf( 43 ) );
		s.close();
	}

	@Test
	public void testParameterHandling() throws HibernateException, SQLException {
		Session s = openSession();

//		Query namedQuery = s.getNamedQuery( "paramhandling" );
		ProcedureCall namedQuery = s.createNamedStoredProcedureQuery( "paramhandling" );
		namedQuery.setParameter( 1, 10 );
		namedQuery.setParameter( 2, 20 );
		List list = namedQuery.getResultList();
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
						session.createNamedStoredProcedureQuery( "paramhandling_mixed" );
						fail( "Expecting an exception" );
					}
					catch (IllegalArgumentException expected) {
						assertEquals(
								"Cannot mix named parameter with positional parameter registrations",
								expected.getMessage()
						);
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
		s.flush();

//		Query namedQuery = s.getNamedQuery( "selectAllEmployments" );
		ProcedureCall namedQuery = s.createNamedStoredProcedureQuery( "selectAllEmployments" );
		List list = namedQuery.getResultList();
		assertTrue( list.get( 0 ) instanceof Employment );
		s.remove( emp );
		s.remove( ifa );
		s.remove( jboss );
		s.remove( gavin );

		t.commit();
		s.close();
	}
}
