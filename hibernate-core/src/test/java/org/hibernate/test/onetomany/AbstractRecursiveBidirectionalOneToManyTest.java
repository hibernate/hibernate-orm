/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetomany;

import java.util.ArrayList;

import org.junit.Test;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 *  What is done:
 *    ___                   ___
 *   |   |                 |   |
 *    -> 1                  -> 1
 *       |   -transform->     / \
 *       2                   2   3
 *       |
 *     	 3
 *
 * @author Burkhard Graves
 * @author Gail Badner
 */
@SuppressWarnings( {"UnusedDeclaration"})
public abstract class AbstractRecursiveBidirectionalOneToManyTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "onetomany/Node.hbm.xml" };
	}

	@Override
	public Session openSession() {
		Session s = super.openSession();
		s.setCacheMode( getSessionCacheMode() );
		return s;
	}

	protected abstract CacheMode getSessionCacheMode();

	@Test
	public void testOneToManyMoveElement() {
		init();
		transformMove();
		check( false );
		delete();
	}

	@Test
	public void testOneToManyMoveElementWithDirtySimpleProperty() {
		init();
		transformMoveWithDirtySimpleProperty();
		check( true );
		delete();
	}

	@Test
	public void testOneToManyReplaceList() {
		init();
		transformReplace();
		check( false );
		delete();
	}

	void init() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Node node1 = new Node( 1, "node1" );
		Node node2 = new Node( 2, "node2" );
		Node node3 = new Node( 3, "node3" );

		node1.addSubNode( node2 );
		node2.addSubNode( node3 );

		s.save(node1);

		tx.commit();
		s.close();
	}

	void transformMove() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Node node3 = (Node) s.load(Node.class, new Integer(3));
		Node node2 = node3.getParentNode();
		Node node1 = node2.getParentNode();

		node2.removeSubNode( node3 );
		node1.addSubNode( node3 );

		tx.commit();
		s.close();
	}

	void transformMoveWithDirtySimpleProperty() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Node node3 = (Node) s.load(Node.class, new Integer(3));
		Node node2 = node3.getParentNode();
		Node node1 = node2.getParentNode();

		node2.removeSubNode( node3 );
		node1.addSubNode( node3 );
		node3.setDescription( "node3-updated" );

		tx.commit();
		s.close();
	}

	void transformReplace() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Node node3 = (Node) s.load(Node.class, new Integer(3));
		Node node2 = node3.getParentNode();
		Node node1 = node2.getParentNode();

		node2.removeSubNode( node3 );
		node1.setSubNodes(  new ArrayList() );
		node1.addSubNode( node2 );
		node1.addSubNode( node3 );

		tx.commit();
		s.close();
	}

	void check(boolean simplePropertyUpdated) {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node node3 = (Node) s.get( Node.class, Integer.valueOf(3) );

		// fails with 2nd level cache enabled
		assertEquals( 1, node3.getParentNode().getId().intValue() );
		assertEquals( ( simplePropertyUpdated ? "node3-updated" : "node3" ), node3.getDescription() );
		assertTrue( node3.getSubNodes().isEmpty() );

		Node node1 = node3.getParentNode();
		assertNull( node1.getParentNode() );
		assertEquals( 2, node1.getSubNodes().size() );
		assertEquals( 2, ( ( Node ) node1.getSubNodes().get( 0 ) ).getId().intValue() );
		assertEquals( "node1", node1.getDescription() );

		Node node2 = ( Node ) node1.getSubNodes().get( 0 );
		assertSame( node1, node2.getParentNode() );
		assertTrue( node2.getSubNodes().isEmpty() );
		assertEquals( "node2", node2.getDescription() );

		tx.commit();
		s.close();
	}

	void delete() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Node node1 = ( Node ) s.get(  Node.class, Integer.valueOf( 1 ) );
		s.delete( node1 );
		tx.commit();
		s.close();
	}
}
