/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2;

import org.hibernate.Session;
import org.hibernate.query.Query;

import org.hibernate.test.jpa.AbstractJPATest;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class ClosedManagerTests extends AbstractJPATest {
	@Test
	public void testQuerySetMaxResults() {
		final Session session = sessionFactory().openSession();

		final Query qry = session.createQuery( "select i from Item i" );

		session.close();
		assertThat( session.isOpen(), CoreMatchers.is ( false ) );

		try {
			qry.setMaxResults( 1 );
			fail( "Expecting call to fail" );
		}
		catch (IllegalStateException expected) {
		}
	}
	@Test
	public void testQuerySetFirstResult() {
		final Session session = sessionFactory().openSession();

		final Query qry = session.createQuery( "select i from Item i" );

		session.close();
		assertThat( session.isOpen(), CoreMatchers.is ( false ) );

		try {
			qry.setFirstResult( 1 );
			fail( "Expecting call to fail" );
		}
		catch (IllegalStateException expected) {
		}
	}

	@Test
	public void testQuerySetParameter() {
		final Session session = sessionFactory().openSession();

		final Query qry = session.createQuery( "select i from Item i where i.id = ?1" );

		session.close();
		assertThat( session.isOpen(), CoreMatchers.is ( false ) );

		try {
			qry.setParameter( 1, 1 );
			fail( "Expecting call to fail" );
		}
		catch (IllegalStateException expected) {
		}
	}

	@Test
	public void testQueryGetParameter() {
		final Session session = sessionFactory().openSession();

		final Query qry = session.createQuery( "select i from Item i where i.id = ?1" );
		qry.setParameter( 1, 1 );

		session.close();
		assertThat( session.isOpen(), CoreMatchers.is ( false ) );

		try {
			qry.getParameter( 1 );
			fail( "Expecting call to fail" );
		}
		catch (IllegalStateException expected) {
		}

		try {
			qry.getParameter( 1, Integer.class );
			fail( "Expecting call to fail" );
		}
		catch (IllegalStateException expected) {
		}
	}
}
