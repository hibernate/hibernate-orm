/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/readonly/VersionedNode.hbm.xml"
)
public class ReadOnlyVersionedNodesTest extends AbstractReadOnlyTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSetReadOnlyTrueAndFalse(SessionFactoryScope scope) {
		VersionedNode n = createVersionNode( scope );

		clearCounts( scope );

		scope.inSession(
				session -> {
					try {
						session.beginTransaction();

						VersionedNode node = session.get( VersionedNode.class, n.getId() );
						session.setReadOnly( node, true );
						node.setName( "node-name" );
						session.getTransaction().commit();

						assertUpdateCount( 0, scope );
						assertInsertCount( 0, scope );

						// the changed name is still in node
						assertEquals( "node-name", node.getName() );

						session.beginTransaction();
						node = session.get( VersionedNode.class, node.getId() );
						// the changed name is still in the session
						assertEquals( "node-name", node.getName() );
						session.refresh( node );
						// after refresh, the name reverts to the original value
						assertEquals( "node", node.getName() );
						node = session.get( VersionedNode.class, node.getId() );
						assertEquals( "node", node.getName() );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );

		scope.inTransaction(
				session -> {
					VersionedNode node = session.get( VersionedNode.class, n.getId() );
					assertEquals( "node", node.getName() );
					session.setReadOnly( node, true );
					node.setName( "diff-node-name" );
					session.flush();
					assertEquals( "diff-node-name", node.getName() );
					session.refresh( node );
					assertEquals( "node", node.getName() );
					session.setReadOnly( node, false );
					node.setName( "diff-node-name" );
				}
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 0, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode node = session.get( VersionedNode.class, n.getId() );
					assertEquals( "diff-node-name", node.getName() );
					assertEquals( 1, node.getVersion() );
					session.setReadOnly( node, true );
					session.remove( node );
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 1, scope );
	}

	@Test
	public void testUpdateSetReadOnlyTwice(SessionFactoryScope scope) {
		VersionedNode n = createVersionNode( scope );

		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode node = session.get( VersionedNode.class, n.getId() );
					node.setName( "node-name" );
					session.setReadOnly( node, true );
					session.setReadOnly( node, true );
				}
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );

		scope.inTransaction(
				session -> {
					VersionedNode node = session.get( VersionedNode.class, n.getId() );
					assertEquals( "node", node.getName() );
					assertEquals( 0, node.getVersion() );
					session.setReadOnly( node, true );
					session.remove( node );
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 1, scope );
	}

	@Test
	public void testUpdateSetModifiable(SessionFactoryScope scope) {
		VersionedNode n = createVersionNode( scope );

		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode node = session.get( VersionedNode.class, n.getId() );
					node.setName( "node-name" );
					session.setReadOnly( node, false );
				}
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 0, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode node = session.get( VersionedNode.class, n.getId() );
					assertEquals( "node-name", node.getName() );
					assertEquals( 1, node.getVersion() );
					session.setReadOnly( node, true );
					session.remove( node );
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 1, scope );
	}

	private VersionedNode createVersionNode(SessionFactoryScope scope) {
		return scope.fromTransaction(
				session -> {
					VersionedNode nd = new VersionedNode( "node", "node" );
					session.persist( nd );
					return nd;
				}
		);
	}

	private VersionedNode createVersionNode(String id, String name, SessionFactoryScope scope) {
		return scope.fromTransaction(
				session -> {
					VersionedNode nd = new VersionedNode( id, name );
					session.persist( nd );
					return nd;
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "unknown")
	public void testUpdateSetReadOnlySetModifiable(SessionFactoryScope scope) {
		VersionedNode n = createVersionNode( scope );

		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode node = session.get( VersionedNode.class, n.getId() );
					node.setName( "node-name" );
					session.setReadOnly( node, true );
					session.setReadOnly( node, false );
				}
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 0, scope );

		scope.inTransaction(
				session -> {
					VersionedNode node = session.get( VersionedNode.class, n.getId() );
					assertEquals( "node-name", node.getName() );
					assertEquals( 1, node.getVersion() );
					session.remove( node );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "unknown")
	public void testSetReadOnlyUpdateSetModifiable(SessionFactoryScope scope) {
		VersionedNode n = createVersionNode( scope );

		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode node = session.get( VersionedNode.class, n.getId() );
					session.setReadOnly( node, true );
					node.setName( "node-name" );
					session.setReadOnly( node, false );
				}
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 0, scope );

		scope.inTransaction(
				session -> {
					VersionedNode node = session.get( VersionedNode.class, n.getId() );
					assertEquals( "node-name", node.getName() );
					assertEquals( 1, node.getVersion() );
					session.remove( node );
				}
		);
	}

	@Test
	public void testAddNewChildToReadOnlyParent(SessionFactoryScope scope) {
		VersionedNode p = createVersionNode( "parent", "parent", scope );

		clearCounts( scope );

		VersionedNode c = scope.fromTransaction(
				session -> {
					VersionedNode parentManaged = session.get( VersionedNode.class, p.getId() );
					session.setReadOnly( parentManaged, true );
					parentManaged.setName( "new parent name" );
					VersionedNode child = new VersionedNode( "child", "child" );
					parentManaged.addChild( child );
					return child;
				}
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 1, scope );

		scope.inTransaction(
				session -> {
					VersionedNode parent = session.get( VersionedNode.class, p.getId() );
					assertEquals( "parent", parent.getName() );
					assertEquals( 1, parent.getChildren().size() );
					assertEquals( 1, parent.getVersion() );
					VersionedNode child = session.get( VersionedNode.class, c.getId() );
					assertNotNull( child );
					session.remove( parent );
				}
		);
	}

	@Test
	public void testUpdateParentWithNewChildCommitWithReadOnlyParent(SessionFactoryScope scope) {
		VersionedNode p = createVersionNode( "parent", "parent", scope );

		clearCounts( scope );

		p.setName( "new parent name" );
		VersionedNode c = new VersionedNode( "child", "child" );
		p.addChild( c );

		scope.inTransaction(
				session -> {
					VersionedNode v = session.merge( p );
					session.setReadOnly( v, true );
				}
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 1, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode parent = session.get( VersionedNode.class, p.getId() );
					VersionedNode child = session.get( VersionedNode.class, c.getId() );
					assertEquals( parent.getName(), "parent" );
					assertEquals( 1, parent.getChildren().size() );
					assertEquals( 1, parent.getVersion() );
					assertSame( parent, child.getParent() );
					assertSame( child, parent.getChildren().iterator().next() );
					assertEquals( 0, child.getVersion() );
					session.setReadOnly( parent, true );
					session.setReadOnly( child, true );
					session.remove( parent );
					session.remove( child );
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 2, scope );
	}

	@Test
	public void testMergeDetachedParentWithNewChildCommitWithReadOnlyParent(SessionFactoryScope scope) {
		VersionedNode p = createVersionNode( "parent", "parent", scope );

		clearCounts( scope );

		p.setName( "new parent name" );
		VersionedNode c = new VersionedNode( "child", "child" );
		p.addChild( c );

		scope.inTransaction(
				session -> {
					VersionedNode parent = (VersionedNode) session.merge( p );
					session.setReadOnly( parent, true );
				}
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 1, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode parent = session.get( VersionedNode.class, p.getId() );
					VersionedNode child = session.get( VersionedNode.class, c.getId() );
					assertEquals( parent.getName(), "parent" );
					assertEquals( 1, parent.getChildren().size() );
					assertEquals( 1, parent.getVersion() );
					assertSame( parent, child.getParent() );
					assertSame( child, parent.getChildren().iterator().next() );
					assertEquals( 0, child.getVersion() );
					session.setReadOnly( parent, true );
					session.setReadOnly( child, true );
					session.remove( parent );
					session.remove( child );
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 2, scope );
	}

	@Test
	public void testGetParentMakeReadOnlyThenMergeDetachedParentWithNewChildC(SessionFactoryScope scope) {
		VersionedNode p = createVersionNode( "parent", "parent", scope );

		clearCounts( scope );

		p.setName( "new parent name" );
		VersionedNode c = new VersionedNode( "child", "child" );
		p.addChild( c );

		scope.inTransaction(
				session -> {
					VersionedNode parentManaged = session.get( VersionedNode.class, p.getId() );
					session.setReadOnly( parentManaged, true );
					VersionedNode parentMerged = (VersionedNode) session.merge( p );
					assertSame( parentManaged, parentMerged );
				}
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 1, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode parent = session.get( VersionedNode.class, p.getId() );
					VersionedNode child = session.get( VersionedNode.class, c.getId() );
					assertEquals( parent.getName(), "parent" );
					assertEquals( 1, parent.getChildren().size() );
					assertEquals( 1, parent.getVersion() );
					assertSame( parent, child.getParent() );
					assertSame( child, parent.getChildren().iterator().next() );
					assertEquals( 0, child.getVersion() );
					session.remove( parent );
					session.remove( child );
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 2, scope );
	}

	@Test
	public void testMergeUnchangedDetachedParentChildren(SessionFactoryScope scope) {
		VersionedNode p = new VersionedNode( "parent", "parent" );
		VersionedNode c = new VersionedNode( "child", "child" );
		scope.inTransaction(
				session -> {
					p.addChild( c );
					session.persist( p );
				}
		);

		clearCounts( scope );

		VersionedNode parent = scope.fromTransaction(
				session ->
						(VersionedNode) session.merge( p )
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode parentGet = session.get( p.getClass(), p.getId() );
					session.merge( parent );
				}
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode parentLoad = session.getReference( parent.getClass(), parent.getId() );
					session.merge( parent );
				}
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode parent_ = session.get( VersionedNode.class, parent.getId() );
					VersionedNode child = session.get( VersionedNode.class, c.getId() );
					assertEquals( parent_.getName(), "parent" );
					assertEquals( 1, parent_.getChildren().size() );
					assertEquals( 0, parent_.getVersion() );
					assertSame( parent_, child.getParent() );
					assertSame( child, parent_.getChildren().iterator().next() );
					assertEquals( 0, child.getVersion() );
					session.remove( parent_ );
					session.remove( child );
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 2, scope );
	}

	@Test
	public void testAddNewParentToReadOnlyChild(SessionFactoryScope scope) {
		VersionedNode c = createVersionNode( "child", "child", scope );

		clearCounts( scope );

		VersionedNode p = new VersionedNode( "parent", "parent" );
		scope.inTransaction(
				session -> {
					VersionedNode childManaged = session.get( VersionedNode.class, c.getId() );
					session.setReadOnly( childManaged, true );
					childManaged.setName( "new child name" );
					p.addChild( childManaged );
				}
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 1, scope );

		scope.inTransaction(
				session -> {
					VersionedNode child = session.get( VersionedNode.class, c.getId() );
					assertEquals( "child", child.getName() );
					assertNull( child.getParent() );
					assertEquals( 0, child.getVersion() );
					VersionedNode parent = session.get( VersionedNode.class, p.getId() );
					assertNotNull( parent );
					session.setReadOnly( child, true );
					session.remove( child );
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 1, scope );
	}

	@Test
	public void testUpdateChildWithNewParentCommitWithReadOnlyChild(SessionFactoryScope scope) {
		VersionedNode c = createVersionNode( "child", "child", scope );


		clearCounts( scope );

		c.setName( "new child name" );
		VersionedNode p = new VersionedNode( "parent", "parent" );
		p.addChild( c );

		scope.inTransaction(
				session -> {
					VersionedNode merged = session.merge( c );
					session.setReadOnly( merged, true );
				}
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 1, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode parent = session.get( VersionedNode.class, p.getId() );
					VersionedNode child = session.get( VersionedNode.class, c.getId() );
					assertEquals( child.getName(), "child" );
					assertNull( child.getParent() );
					assertEquals( 0, child.getVersion() );
					assertNotNull( parent );
					assertEquals( 0, parent.getChildren().size() );
					assertEquals( 1, parent.getVersion() );
					session.setReadOnly( parent, true );
					session.setReadOnly( child, true );
					session.remove( parent );
					session.remove( child );
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 2, scope );
	}

	@Test
	public void testMergeDetachedChildWithNewParentCommitWithReadOnlyChild(SessionFactoryScope scope) {
		VersionedNode c = createVersionNode( "child", "child", scope );

		clearCounts( scope );

		c.setName( "new child name" );
		VersionedNode p = new VersionedNode( "parent", "parent" );
		p.addChild( c );

		scope.inTransaction(
				session -> {
					VersionedNode child = (VersionedNode) session.merge( c );
					session.setReadOnly( child, true );
				}
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 1, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode parent = session.get( VersionedNode.class, p.getId() );
					VersionedNode child = session.get( VersionedNode.class, c.getId() );
					assertEquals( child.getName(), "child" );
					assertNull( child.getParent() );
					assertEquals( 0, child.getVersion() );
					assertNotNull( parent );
					assertEquals( 0, parent.getChildren().size() );
					assertEquals( 1, parent.getVersion() );    // hmmm, why was version updated?
					session.setReadOnly( parent, true );
					session.setReadOnly( child, true );
					session.remove( parent );
					session.remove( child );
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 2, scope );
	}

	@Test
	public void testGetChildMakeReadOnlyThenMergeDetachedChildWithNewParent(SessionFactoryScope scope) {
		VersionedNode c = createVersionNode( "child", "child", scope );

		clearCounts( scope );

		c.setName( "new child name" );
		VersionedNode p = new VersionedNode( "parent", "parent" );
		p.addChild( c );

		scope.inTransaction(
				session -> {
					VersionedNode childManaged = session.get( VersionedNode.class, c.getId() );
					session.setReadOnly( childManaged, true );
					VersionedNode childMerged = (VersionedNode) session.merge( c );
					assertSame( childManaged, childMerged );
				}
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 1, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					VersionedNode parent = session.get( VersionedNode.class, p.getId() );
					VersionedNode child = session.get( VersionedNode.class, c.getId() );
					assertEquals( child.getName(), "child" );
					assertNull( child.getParent() );
					assertEquals( 0, child.getVersion() );
					assertNotNull( parent );
					assertEquals( 0, parent.getChildren().size() );
					assertEquals( 1, parent.getVersion() ); // / hmmm, why was version updated?
					session.setReadOnly( parent, true );
					session.setReadOnly( child, true );
					session.remove( parent );
					session.remove( child );
				}
		);

		assertUpdateCount( 0, scope );
		assertDeleteCount( 2, scope );
	}

	protected void cleanupTest(SessionFactoryScope scope) throws Exception {
		cleanup( scope );
	}

	private void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from VersionedNode where parent is not null" ).executeUpdate();
					session.createQuery( "delete from VersionedNode" ).executeUpdate();
				}
		);
	}
}
