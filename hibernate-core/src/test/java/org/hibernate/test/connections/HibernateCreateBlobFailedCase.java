/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.connections;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;

/**
 * Test originally developed to verify and fix HHH-5550
 *
 * @author Steve Ebersole
 */
public class HibernateCreateBlobFailedCase extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "connections/Silly.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread" );
	}

	@Test
	public void testLobCreation() throws SQLException {
		Session session = sessionFactory().getCurrentSession();
		session.beginTransaction();
		Blob blob = Hibernate.getLobCreator( session ).createBlob( new byte[] {} );
		blob.free();
		Clob clob = Hibernate.getLobCreator( session ).createClob( "Steve" );
		clob.free();
		session.getTransaction().commit();
		assertFalse( session.isOpen() );
	}

}
