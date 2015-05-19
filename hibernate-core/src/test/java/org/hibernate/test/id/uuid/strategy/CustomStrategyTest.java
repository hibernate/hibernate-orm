/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.uuid.strategy;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
/**
 * @author Steve Ebersole
 */
public class CustomStrategyTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "id/uuid/strategy/Node.hbm.xml" };
	}

	@Test
	public void testUsage() {
		Session session = openSession();
		session.beginTransaction();
		Node node = new Node();
		session.save( node );
		assertNotNull( node.getId() );
		assertEquals( 2, node.getId().variant() );
		assertEquals( 1, node.getId().version() );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.delete( node );
		session.getTransaction().commit();
		session.close();
	}
}
