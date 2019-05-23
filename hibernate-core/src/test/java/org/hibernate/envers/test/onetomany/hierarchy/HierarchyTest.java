/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.hierarchy;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.hierarchy.Node;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6661")
public class HierarchyTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long parentId = null;
	private Long child1Id = null;
	private Long child2Id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Node.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					Node parent = new Node( "parent", (Node) null );
					Node child1 = new Node( "child1", parent );
					Node child2 = new Node( "child2", parent );
					parent.getChildren().add( child1 );
					parent.getChildren().add( child2 );
					entityManager.persist( parent );
					entityManager.persist( child1 );
					entityManager.persist( child2 );

					parentId = parent.getId();
					child1Id = child1.getId();
					child2Id = child2.getId();
				},

				// Revision 2
				entityManager -> {
					Node parent = entityManager.find( Node.class, parentId );
					parent.getChildren().get( 0 ).setData( "child1 modified" );
				},

				// Revision 3
				entityManager -> {
					Node child2 = entityManager.find( Node.class, child2Id );
					entityManager.remove( child2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( Node.class, parentId ), contains( 1, 3 ) );
		assertThat( getAuditReader().getRevisions( Node.class, child1Id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( Node.class, child2Id ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfParentNode() {
		Node parent = new Node( "parent", parentId );
		Node child1 = new Node( "child1", child1Id );
		Node child2 = new Node( "child2", child2Id );

		Node rev1 = getAuditReader().find( Node.class, parentId, 1 );
		assertThat( rev1, equalTo( parent ) );
		assertThat( rev1.getChildren(), containsInAnyOrder( child1, child2 ) );

		child1.setData( "child1 modified" );

		Node rev2 = getAuditReader().find( Node.class, parentId, 2 );
		assertThat( rev2, equalTo( parent ) );
		assertThat( rev2.getChildren(), containsInAnyOrder( child1, child2 ) );

		Node rev3 = getAuditReader().find( Node.class, parentId, 3 );
		assertThat( rev3, equalTo( parent ) );
		assertThat( rev3.getChildren(), containsInAnyOrder( child1 ) );
	}

	@DynamicTest
	public void testHistoryOfChild1Node() {
		Node parent = new Node( "parent", parentId );
		Node child1 = new Node( "child1", child1Id );

		Node rev1 = getAuditReader().find( Node.class, child1Id, 1 );
		assertThat( rev1, equalTo( child1 ) );
		assertThat( rev1.getParent().getId(), equalTo( parent.getId() ) );
		assertThat( rev1.getParent().getData(), equalTo( parent.getData() ) );

		child1.setData( "child1 modified" );

		Node rev2 = getAuditReader().find( Node.class, child1Id, 2 );
		assertThat( rev2, equalTo( child1 ) );
		assertThat( rev2.getParent().getId(), equalTo( parent.getId() ) );
		assertThat( rev2.getParent().getData(), equalTo( parent.getData() ) );
	}
}