/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany;

import java.util.ArrayList;

import org.hibernate.CacheMode;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What is done:
 * ___                   ___
 * |   |                 |   |
 * -> 1                  -> 1
 * |   -transform->     / \
 * 2                   2   3
 * |
 * 3
 *
 * @author Burkhard Graves
 * @author Gail Badner
 */
@SuppressWarnings("unused")
public abstract class AbstractRecursiveBidirectionalOneToManyTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/onetomany/Node.hbm.xml" };
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
		inTransaction(
				session -> {
					session.setCacheMode( getSessionCacheMode() );
					Node node1 = new Node( 1, "node1" );
					Node node2 = new Node( 2, "node2" );
					Node node3 = new Node( 3, "node3" );

					node1.addSubNode( node2 );
					node2.addSubNode( node3 );

					session.persist( node1 );
				}
		);
	}

	void transformMove() {
		inTransaction(
				session -> {
					session.setCacheMode( getSessionCacheMode() );
					Node node3 = session.getReference( Node.class, new Integer( 3 ) );
					Node node2 = node3.getParentNode();
					Node node1 = node2.getParentNode();

					node2.removeSubNode( node3 );
					node1.addSubNode( node3 );
				}
		);
	}

	void transformMoveWithDirtySimpleProperty() {
		inTransaction(
				session -> {
					session.setCacheMode( getSessionCacheMode() );
					Node node3 = session.getReference( Node.class, new Integer( 3 ) );
					Node node2 = node3.getParentNode();
					Node node1 = node2.getParentNode();

					node2.removeSubNode( node3 );
					node1.addSubNode( node3 );
					node3.setDescription( "node3-updated" );
				}
		);
	}

	void transformReplace() {
		inTransaction(
				session -> {
					session.setCacheMode( getSessionCacheMode() );
					Node node3 = session.getReference( Node.class, new Integer( 3 ) );
					Node node2 = node3.getParentNode();
					Node node1 = node2.getParentNode();

					node2.removeSubNode( node3 );
					node1.setSubNodes( new ArrayList() );
					node1.addSubNode( node2 );
					node1.addSubNode( node3 );
				}
		);
	}

	void check(boolean simplePropertyUpdated) {
		inTransaction(
				session -> {
					session.setCacheMode( getSessionCacheMode() );
					Node node3 = session.get( Node.class, Integer.valueOf( 3 ) );

					// fails with 2nd level cache enabled
					assertEquals( 1, node3.getParentNode().getId().intValue() );
					assertEquals( ( simplePropertyUpdated ? "node3-updated" : "node3" ), node3.getDescription() );
					assertTrue( node3.getSubNodes().isEmpty() );

					Node node1 = node3.getParentNode();
					assertNull( node1.getParentNode() );
					assertEquals( 2, node1.getSubNodes().size() );
					assertEquals( 2, ( (Node) node1.getSubNodes().get( 0 ) ).getId().intValue() );
					assertEquals( "node1", node1.getDescription() );

					Node node2 = (Node) node1.getSubNodes().get( 0 );
					assertSame( node1, node2.getParentNode() );
					assertTrue( node2.getSubNodes().isEmpty() );
					assertEquals( "node2", node2.getDescription() );
				}
		);
	}

	void delete() {
		inTransaction(
				session -> {
					session.setCacheMode( getSessionCacheMode() );
					Node node1 = session.get( Node.class, Integer.valueOf( 1 ) );
					session.remove( node1 );
				}
		);
	}
}
