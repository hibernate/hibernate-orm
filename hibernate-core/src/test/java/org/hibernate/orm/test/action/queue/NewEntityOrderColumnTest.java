/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.action.queue.QueueType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// Tests for @OrderColumn with new entities in GraphBasedActionQueue.
///
/// Tests three scenarios:
/// 1. Simple case: NEW entity with pre-populated @OrderColumn collection → PASSES ✓
/// 2. Complex case: Replace existing entity + NEW entity with collection → PASSES ✓
/// 3. Multiple children: NEW entity with multiple @OrderColumn elements → PASSES ✓
///
/// The complex case (scenario 2) reproduces the orphan removal + new entity insertion scenario
/// from DeleteMultiLevelOrphansTest.testReplacedDirectAssociationWhileManaged.
///
/// SQL execution sequence for the complex case:
/// 1. DELETE old parent/children (DELETE→INSERT edge ensures this is first)
/// 2. INSERT new Parent (id=2)
/// 3. INSERT new Child (id=3, parent_id=2)
/// 4. UPDATE Child SET children_ORDER=0 WHERE id=3 (WriteIndexCoordinator)
///
/// The DELETE→INSERT dependency edges in StandardGraphBuilder ensure proper table-level
/// ordering, allowing the UPDATE to successfully locate newly inserted rows.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
		@Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
})
@DomainModel(
		annotatedClasses = {
				NewEntityOrderColumnTest.Parent.class,
				NewEntityOrderColumnTest.Child.class
		}
)
@SessionFactory(
		generateStatistics = true
)
@JiraKey("HHH-9091")
public class NewEntityOrderColumnTest {

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.dropData();
	}

	/// Tests that @OrderColumn is properly set for NEW entities with pre-populated collections.
	/// Status: PASSES ✓ (simple case works correctly)
	@Test
	public void testNewEntityWithPrePopulatedOrderColumn(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long parentId = scope.fromTransaction( session -> {
			// Create NEW parent with NEW child in collection
			Parent parent = new Parent();
			parent.name = "parent";

			Child child = new Child();
			child.name = "child";

			// Pre-populate collection BEFORE persist
			parent.children.add( child );
			child.parent = parent;

			session.persist( parent );

			return parent.id;
		} );

		// Load in new transaction - this should work but currently fails
		scope.inTransaction( session -> {
			Parent parent = session.find( Parent.class, parentId );
			assertNotNull( parent );

			// This fails with: Illegal null value for list index encountered while reading
			assertEquals( 1, parent.children.size() );
			assertEquals( "child", parent.children.get( 0 ).name );
		} );
	}

	/// Tests replacing an existing parent with a new parent (orphan removal + new entity).
	/// This reproduces the exact scenario from DeleteMultiLevelOrphansTest.testReplacedDirectAssociationWhileManaged.
	/// Status: PASSES ✓ (fixed by DELETE→INSERT dependency edges in GraphBasedActionQueue)
	@Test
	public void testReplaceParentWithNewParentAndChildren(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Setup: Create initial parent with children
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.name = "oldParent";

			Child child1 = new Child();
			child1.name = "oldChild1";
			parent.children.add( child1 );
			child1.parent = parent;

			Child child2 = new Child();
			child2.name = "oldChild2";
			parent.children.add( child2 );
			child2.parent = parent;

			session.persist( parent );
		} );

		// Replace: Delete old parent and create new parent with new children
		scope.inTransaction( session -> {
			// Load and delete old parent (orphan removal deletes old children)
			Parent oldParent = session.createQuery( "from Parent", Parent.class ).getSingleResult();
			session.remove( oldParent );

			// Create NEW parent with NEW children
			Parent newParent = new Parent();
			newParent.name = "newParent";

			Child newChild = new Child();
			newChild.name = "newChild";
			newParent.children.add( newChild );
			newChild.parent = newParent;

			session.persist( newParent );
		} );

		// Verify: Load in new transaction
		scope.inTransaction( session -> {
			List<Parent> parents = session.createQuery( "from Parent", Parent.class ).getResultList();
			assertEquals( 1, parents.size() );

			Parent parent = parents.get( 0 );
			assertEquals( "newParent", parent.name );

			// This fails with: Illegal null value for list index
			assertEquals( 1, parent.children.size() );
			assertEquals( "newChild", parent.children.get( 0 ).name );
		} );
	}

	/// Tests adding multiple children to verify ORDER column indexing.
	/// Status: PASSES ✓ (simple case with multiple elements works)
	@Test
	public void testNewEntityWithMultipleChildren(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long parentId = scope.fromTransaction( session -> {
			Parent parent = new Parent();
			parent.name = "parent";

			Child child1 = new Child();
			child1.name = "child1";
			parent.children.add( child1 );
			child1.parent = parent;

			Child child2 = new Child();
			child2.name = "child2";
			parent.children.add( child2 );
			child2.parent = parent;

			session.persist( parent );

			return parent.id;
		} );

		scope.inTransaction( session -> {
			Parent parent = session.find( Parent.class, parentId );
			assertEquals( 2, parent.children.size() );
			assertEquals( "child1", parent.children.get( 0 ).name );
			assertEquals( "child2", parent.children.get( 1 ).name );
		} );
	}

	@Entity(name = "Parent")
	@Table(name = "new_entity_parent")
	public static class Parent {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", orphanRemoval = true)
		@OrderColumn(name = "children_ORDER")
		List<Child> children = new ArrayList<>();
	}

	@Entity(name = "Child")
	@Table(name = "new_entity_child")
	public static class Child {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@ManyToOne(fetch = FetchType.LAZY)
		Parent parent;
	}
}
