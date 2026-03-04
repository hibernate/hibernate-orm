/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.integration;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for mixed operations (insert + update + delete) in same flush.
 * Tests the complete decomposition → execution → finalization flow.
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		MixedOperationsTest.SimpleEntity.class,
		MixedOperationsTest.Parent.class,
		MixedOperationsTest.Child.class,
		MixedOperationsTest.VersionedEntity.class
})
public class MixedOperationsTest {

	@Test
	public void testInsertUpdateDeleteInSameFlush(EntityManagerFactoryScope scope) {
		// Setup: Create entity to update and entity to delete
		Long updateId = scope.fromTransaction( em -> {
			SimpleEntity toUpdate = new SimpleEntity();
			toUpdate.name = "ToUpdate";

			SimpleEntity toDelete = new SimpleEntity();
			toDelete.name = "ToDelete";

			em.persist( toUpdate );
			em.persist( toDelete );
			em.flush();

			return toUpdate.id;
		} );

		Long deleteId = updateId + 1;

		// Test: Insert, update, and delete in same flush
		scope.inTransaction( em -> {
			// INSERT
			SimpleEntity newEntity = new SimpleEntity();
			newEntity.name = "New";
			em.persist( newEntity );

			// UPDATE
			SimpleEntity toUpdate = em.find( SimpleEntity.class, updateId );
			toUpdate.name = "Updated";

			// DELETE
			SimpleEntity toDelete = em.find( SimpleEntity.class, deleteId );
			em.remove( toDelete );

			em.flush();

			// Verify all operations executed
			assertNotNull( newEntity.id, "INSERT should have generated ID" );
			assertEquals( "Updated", toUpdate.name );
			assertFalse( em.contains( toDelete ), "DELETE should remove from context" );
		} );

		// Verify persistence
		scope.inTransaction( em -> {
			assertNotNull( em.find( SimpleEntity.class, updateId ), "Updated entity should exist" );
			assertNull( em.find( SimpleEntity.class, deleteId ), "Deleted entity should not exist" );
		} );
	}

	@Test
	public void testInsertThenUpdate(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Insert entity
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Initial";
			em.persist( entity );
			em.flush();

			assertNotNull( entity.id, "ID should be generated after first flush" );
			Long entityId = entity.id;

			// Update same entity in same transaction
			entity.name = "Updated";
			em.flush();

			// Verify update persisted
			em.clear();
			SimpleEntity reloaded = em.find( SimpleEntity.class, entityId );
			assertEquals( "Updated", reloaded.name );
		} );
	}

	@Test
	public void testInsertThenDelete(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Insert entity
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Temporary";
			em.persist( entity );
			em.flush();

			assertNotNull( entity.id );
			Long entityId = entity.id;

			// Delete same entity in same transaction
			em.remove( entity );
			em.flush();

			// Verify entity is deleted
			SimpleEntity found = em.find( SimpleEntity.class, entityId );
			assertNull( found, "Entity should be deleted" );
		} );
	}

	@Test
	public void testUpdateThenDelete(EntityManagerFactoryScope scope) {
		Long entityId = scope.fromTransaction( em -> {
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Initial";
			em.persist( entity );
			em.flush();
			return entity.id;
		} );

		scope.inTransaction( em -> {
			SimpleEntity entity = em.find( SimpleEntity.class, entityId );

			// Update
			entity.name = "Updated";
			em.flush();

			// Then delete
			em.remove( entity );
			em.flush();
		} );

		// Verify entity is deleted
		scope.inTransaction( em -> {
			assertNull( em.find( SimpleEntity.class, entityId ) );
		} );
	}

	@Test
	public void testInsertParentThenChild(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Insert parent
			Parent parent = new Parent();
			parent.name = "Parent";
			em.persist( parent );

			// Insert child referencing parent (in same flush)
			Child child = new Child();
			child.name = "Child";
			child.parent = parent;
			em.persist( child );

			em.flush();

			// Verify FK relationship established
			assertNotNull( parent.id );
			assertNotNull( child.id );
			assertSame( parent, child.parent );
		} );
	}

	@Test
	public void testInsertChildBeforeParent(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create parent (transient)
			Parent parent = new Parent();
			parent.name = "Parent";

			// Insert child first (references transient parent)
			Child child = new Child();
			child.name = "Child";
			child.parent = parent;
			em.persist( child );

			// Insert parent
			em.persist( parent );

			em.flush();

			// Decomposer should handle ordering correctly
			assertNotNull( parent.id, "Parent should be inserted first" );
			assertNotNull( child.id, "Child should be inserted with FK to parent" );
			assertEquals( parent.id, child.parent.id );
		} );
	}

	@Test
	public void testVersionedEntityMixedOperations(EntityManagerFactoryScope scope) {
		Long id1 = scope.fromTransaction( em -> {
			VersionedEntity entity = new VersionedEntity();
			entity.name = "Entity1";
			em.persist( entity );
			em.flush();
			return entity.id;
		} );

		scope.inTransaction( em -> {
			// INSERT new versioned entity
			VersionedEntity newEntity = new VersionedEntity();
			newEntity.name = "NewEntity";
			em.persist( newEntity );

			// UPDATE existing versioned entity
			VersionedEntity existing = em.find( VersionedEntity.class, id1 );
			Long oldVersion = existing.version;
			existing.name = "Updated";

			em.flush();

			// Verify versions
			assertNotNull( newEntity.version );
			assertEquals( 0L, newEntity.version, "New entity version should be 0" );

			assertEquals( oldVersion + 1, existing.version, "Updated entity version should increment" );
		} );
	}

	@Test
	public void testMultipleUpdatesAndDeletesInSameFlush(EntityManagerFactoryScope scope) {
		List<Long> ids = scope.fromTransaction( em -> {
			List<Long> result = new ArrayList<>();
			for ( int i = 0; i < 5; i++ ) {
				SimpleEntity entity = new SimpleEntity();
				entity.name = "Entity" + i;
				em.persist( entity );
				em.flush();
				result.add( entity.id );
			}
			return result;
		} );

		scope.inTransaction( em -> {
			// Update entities 0, 1, 2
			em.find( SimpleEntity.class, ids.get( 0 ) ).name = "Updated0";
			em.find( SimpleEntity.class, ids.get( 1 ) ).name = "Updated1";
			em.find( SimpleEntity.class, ids.get( 2 ) ).name = "Updated2";

			// Delete entities 3, 4
			em.remove( em.find( SimpleEntity.class, ids.get( 3 ) ) );
			em.remove( em.find( SimpleEntity.class, ids.get( 4 ) ) );

			em.flush();
		} );

		// Verify
		scope.inTransaction( em -> {
			assertEquals( "Updated0", em.find( SimpleEntity.class, ids.get( 0 ) ).name );
			assertEquals( "Updated1", em.find( SimpleEntity.class, ids.get( 1 ) ).name );
			assertEquals( "Updated2", em.find( SimpleEntity.class, ids.get( 2 ) ).name );
			assertNull( em.find( SimpleEntity.class, ids.get( 3 ) ) );
			assertNull( em.find( SimpleEntity.class, ids.get( 4 ) ) );
		} );
	}

	@Test
	public void testComplexMixedOperationsWithRelationships(EntityManagerFactoryScope scope) {
		Long parentId = scope.fromTransaction( em -> {
			Parent parent = new Parent();
			parent.name = "Parent";

			Child child1 = new Child();
			child1.name = "Child1";
			child1.parent = parent;

			em.persist( parent );
			em.persist( child1 );
			em.flush();

			return parent.id;
		} );

		scope.inTransaction( em -> {
			Parent parent = em.find( Parent.class, parentId );

			// INSERT new child
			Child newChild = new Child();
			newChild.name = "NewChild";
			newChild.parent = parent;
			em.persist( newChild );

			// UPDATE parent
			parent.name = "UpdatedParent";

			// DELETE will be tested separately due to cascade complexity

			em.flush();

			assertNotNull( newChild.id );
			assertEquals( "UpdatedParent", parent.name );
		} );
	}

	// Test entities

	@Entity(name = "SimpleEntity")
	@Table(name = "mixed_ops_simple")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;
	}

	@Entity(name = "Parent")
	@Table(name = "mixed_ops_parent")
	public static class Parent {
		@Id
		@GeneratedValue
		Long id;

		String name;
	}

	@Entity(name = "Child")
	@Table(name = "mixed_ops_child")
	public static class Child {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@ManyToOne
		@JoinColumn(name = "parent_id")
		Parent parent;
	}

	@Entity(name = "VersionedEntity")
	@Table(name = "mixed_ops_versioned")
	public static class VersionedEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@Version
		Long version;
	}
}
