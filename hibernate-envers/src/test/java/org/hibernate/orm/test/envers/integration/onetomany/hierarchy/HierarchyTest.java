/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.hierarchy;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6661")
@EnversTest
@Jpa(annotatedClasses = {Node.class})
public class HierarchyTest {
	private Long parentId = null;
	private Long child1Id = null;
	private Long child2Id = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Revision 1
			Node parent = new Node( "parent", (Node) null );
			Node child1 = new Node( "child1", parent );
			Node child2 = new Node( "child2", parent );
			parent.getChildren().add( child1 );
			parent.getChildren().add( child2 );
			em.persist( parent );
			em.persist( child1 );
			em.persist( child2 );

			parentId = parent.getId();
			child1Id = child1.getId();
			child2Id = child2.getId();
		} );

		scope.inTransaction( em -> {
			// Revision 2
			Node parent = em.find( Node.class, parentId );
			parent.getChildren().get( 0 ).setData( "child1 modified" );
		} );

		scope.inTransaction( em -> {
			// Revision 3
			Node child2 = em.find( Node.class, child2Id );
			em.remove( child2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( Node.class, parentId ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( Node.class, child1Id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( Node.class, child2Id ) );
		} );
	}

	@Test
	public void testHistoryOfParentNode(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			Node parent = new Node( "parent", parentId );
			Node child1 = new Node( "child1", child1Id );
			Node child2 = new Node( "child2", child2Id );

			Node ver1 = auditReader.find( Node.class, parentId, 1 );
			assertEquals( parent, ver1 );
			assertTrue( TestTools.checkCollection( ver1.getChildren(), child1, child2 ) );

			child1.setData( "child1 modified" );

			Node ver2 = auditReader.find( Node.class, parentId, 2 );
			assertEquals( parent, ver2 );
			assertTrue( TestTools.checkCollection( ver2.getChildren(), child1, child2 ) );

			Node ver3 = auditReader.find( Node.class, parentId, 3 );
			assertEquals( parent, ver3 );
			assertTrue( TestTools.checkCollection( ver3.getChildren(), child1 ) );
		} );
	}

	@Test
	public void testHistoryOfChild1Node(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			Node parent = new Node( "parent", parentId );
			Node child1 = new Node( "child1", child1Id );

			Node ver1 = auditReader.find( Node.class, child1Id, 1 );
			assertEquals( child1, ver1 );
			assertEquals( parent.getId(), ver1.getParent().getId() );
			assertEquals( parent.getData(), ver1.getParent().getData() );

			child1.setData( "child1 modified" );

			Node ver2 = auditReader.find( Node.class, child1Id, 2 );
			assertEquals( child1, ver2 );
			assertEquals( parent.getId(), ver2.getParent().getId() );
			assertEquals( parent.getData(), ver2.getParent().getData() );
		} );
	}
}
