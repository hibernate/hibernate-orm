/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.bytecode;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class ProxyBreakingTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testProxiedBridgeMethod() throws Exception {
		//bridge methods should not be proxied
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Hammer h = new Hammer();
		s.persist(h);
		s.flush();
		s.clear();
		assertNotNull( "The proxy creation failure is breaking things", h.getId() );
		h = (Hammer) s.getReference( Hammer.class, h.getId() );
		assertFalse( Hibernate.isInitialized( h ) );
		tx.rollback();
		s.close();
	}

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] {
				"org/hibernate/orm/test/annotations/bytecode/Hammer.hbm.xml"
		};
	}
}
