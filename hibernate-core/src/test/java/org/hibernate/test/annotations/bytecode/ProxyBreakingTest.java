/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.bytecode;

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
		s.save(h);
		s.flush();
		s.clear();
		assertNotNull( "The proxy creation failure is breaking things", h.getId() );
		h = (Hammer) s.load( Hammer.class, h.getId() );
		assertFalse( Hibernate.isInitialized( h ) );
		tx.rollback();
		s.close();
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[] {
				"org/hibernate/test/annotations/bytecode/Hammer.hbm.xml"
		};
	}
}
