/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.id.uuid.sqlrep.sqlbinary;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.test.id.uuid.sqlrep.Node;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

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
