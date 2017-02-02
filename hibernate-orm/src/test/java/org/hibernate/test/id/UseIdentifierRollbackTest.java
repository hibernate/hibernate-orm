/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.id;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Emmanuel Bernard
 */
public class UseIdentifierRollbackTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "id/Product.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.USE_IDENTIFIER_ROLLBACK, "true");
		super.configure( cfg );
	}

	@Test
	public void testSimpleRollback() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product();
		assertNull( prod.getName() );
		session.persist(prod);
		session.flush();
		assertNotNull( prod.getName() );
		t.rollback();
		session.close();
	}
}
