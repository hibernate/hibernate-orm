/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.uuid.sqlrep.sqlbinary;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.id.uuid.sqlrep.Node;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class UUIDBinaryTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "id/uuid/sqlrep/Node.hbm.xml" };
	}

	@Test
	public void testUsage() {
		Session session = openSession();
		session.beginTransaction();
		Node root = new Node( "root" );
		session.save( root );
		assertNotNull( root.getId() );
		Node child = new Node( "child", root );
		session.save( child );
		assertNotNull( child.getId() );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		Node node = (Node) session.get( Node.class, root.getId() );
		assertNotNull( node );
		node = (Node) session.get( Node.class, child.getId() );
		assertNotNull( node );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		// test joining
		node = (Node) session.createQuery( "from Node n join fetch n.parent where n.parent is not null" ).uniqueResult();
		assertNotNull( node );
		assertNotNull( node.getParent() );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.delete( child );
		session.delete( root );
		session.getTransaction().commit();
		session.close();
	}
}
