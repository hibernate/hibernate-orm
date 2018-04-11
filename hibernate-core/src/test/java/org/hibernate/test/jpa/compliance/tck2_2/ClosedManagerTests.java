/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
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
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.JPA_CLOSED_COMPLIANCE, "true" );
	}

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
	public void testQuerySetPositionalParameter() {
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
	public void testQuerySetNamedParameter() {
		final Session session = sessionFactory().openSession();

		final Query qry = session.createQuery( "select i from Item i where i.id = :id" );

		session.close();
		assertThat( session.isOpen(), CoreMatchers.is ( false ) );

		try {
			qry.setParameter( "id", 1 );
			fail( "Expecting call to fail" );
		}
		catch (IllegalStateException expected) {
		}
	}

	@Test
	public void testQueryGetPositionalParameter() {
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

	@Test
	public void testQueryGetNamedParameter() {
		final Session session = sessionFactory().openSession();

		final Query qry = session.createQuery( "select i from Item i where i.id = :id" );
		qry.setParameter( "id", 1 );

		session.close();
		assertThat( session.isOpen(), CoreMatchers.is ( false ) );

		try {
			qry.getParameter( "id" );
			fail( "Expecting call to fail" );
		}
		catch (IllegalStateException expected) {
		}

		try {
			qry.getParameter( "id", Long.class );
			fail( "Expecting call to fail" );
		}
		catch (IllegalStateException expected) {
		}
	}

	@Test
	public void testClose() {
		final Session session = sessionFactory().openSession();

		// 1st call - should be ok
		session.close();

		try {
			// 2nd should fail (JPA compliance enabled)
			session.close();

			fail();
		}
		catch (IllegalStateException expected) {
			// expected outcome
		}
	}
}
