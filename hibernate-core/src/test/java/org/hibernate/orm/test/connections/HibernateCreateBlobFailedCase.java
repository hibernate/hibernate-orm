/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.Hibernate.getLobHelper;
import static org.junit.Assert.assertFalse;

/**
 * Test originally developed to verify and fix HHH-5550
 *
 * @author Steve Ebersole
 */
public class HibernateCreateBlobFailedCase extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

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
		Blob blob = getLobHelper().createBlob( new byte[] {} );
		blob.free();
		Clob clob = getLobHelper().createClob( "Steve" );
		clob.free();
		session.getTransaction().commit();
		assertFalse( session.isOpen() );
	}

}
