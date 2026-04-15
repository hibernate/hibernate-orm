/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.callback;

import org.hibernate.action.queue.QueueType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PostDeleteHandling post-execution callback.
 * Verifies that all finalization work is properly executed after DELETE operations.
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		PostDeleteHandlingTest.SimpleEntity.class,
		PostDeleteHandlingTest.EntityWithVersion.class,
		PostDeleteHandlingTest.EntityWithCallbacks.class
})
public class PostDeleteHandlingTest {

	@Test
	public void testDeleteRemovesEntityFromPersistenceContext(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long entityId = scope.fromTransaction( entityManager -> {
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Test";

			entityManager.persist( entity );
			entityManager.flush();

			return entity.id;
		} );

		scope.inTransaction( entityManager -> {
			SimpleEntity entity = entityManager.find( SimpleEntity.class, entityId );
			assertNotNull( entity );

			// Entity should be managed
			assertTrue( entityManager.contains( entity ),
				"Entity should be in persistence context before delete" );

			entityManager.remove( entity );
			entityManager.flush();

			// PostDeleteHandling should have removed entity from persistence context
			assertFalse( entityManager.contains( entity ),
				"Entity should be removed from persistence context after delete" );
		} );
	}

	@Test
	public void testPostRemoveCallbackFired(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			EntityWithCallbacks entity = new EntityWithCallbacks();
			entity.name = "Test";

			entityManager.persist( entity );
			entityManager.flush();

			// Callbacks should not have fired yet
			assertFalse( entity.preRemoveCalled );
			assertFalse( entity.postRemoveCalled );

			entityManager.remove( entity );

			// PreRemove should fire on remove()
			assertTrue( entity.preRemoveCalled, "PreRemove should be called on remove()" );
			assertFalse( entity.postRemoveCalled, "PostRemove should not be called yet" );

			entityManager.flush();

			// PostDeleteHandling should fire PostRemove event
			assertTrue( entity.postRemoveCalled, "PostRemove should be called after flush" );
		} );
	}

	@Test
	public void testDeleteActuallyRemovesRow(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long entityId = scope.fromTransaction( entityManager -> {
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Test";

			entityManager.persist( entity );
			entityManager.flush();

			return entity.id;
		} );

		// Delete the entity
		scope.inTransaction( entityManager -> {
			SimpleEntity entity = entityManager.find( SimpleEntity.class, entityId );
			assertNotNull( entity );

			entityManager.remove( entity );
			entityManager.flush();
		} );

		// Verify entity is deleted in new transaction
		scope.inTransaction( entityManager -> {
			SimpleEntity entity = entityManager.find( SimpleEntity.class, entityId );
			assertNull( entity, "Entity should be deleted from database" );
		} );
	}

	@Test
	public void testDeleteVersionedEntity(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long entityId = scope.fromTransaction( entityManager -> {
			EntityWithVersion entity = new EntityWithVersion();
			entity.name = "Test";

			entityManager.persist( entity );
			entityManager.flush();

			// Version should be set
			assertNotNull( entity.version );
			assertEquals( 0L, entity.version );

			return entity.id;
		} );

		// Delete the entity
		scope.inTransaction( entityManager -> {
			EntityWithVersion entity = entityManager.find( EntityWithVersion.class, entityId );
			assertNotNull( entity );

			Long versionBeforeDelete = entity.version;
			assertNotNull( versionBeforeDelete );

			entityManager.remove( entity );
			entityManager.flush();

			// Version should remain unchanged after delete
			assertEquals( versionBeforeDelete, entity.version,
				"Version should not change on delete" );
		} );

		// Verify entity is deleted
		scope.inTransaction( entityManager -> {
			EntityWithVersion entity = entityManager.find( EntityWithVersion.class, entityId );
			assertNull( entity, "Versioned entity should be deleted" );
		} );
	}

	@Test
	public void testMultipleDeletesInSameTransaction(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long id1 = scope.fromTransaction( entityManager -> {
			SimpleEntity entity1 = new SimpleEntity();
			entity1.name = "Entity1";

			SimpleEntity entity2 = new SimpleEntity();
			entity2.name = "Entity2";

			SimpleEntity entity3 = new SimpleEntity();
			entity3.name = "Entity3";

			entityManager.persist( entity1 );
			entityManager.persist( entity2 );
			entityManager.persist( entity3 );
			entityManager.flush();

			return entity1.id;
		} );

		Long id2 = id1 + 1;
		Long id3 = id1 + 2;

		// Delete all three
		scope.inTransaction( entityManager -> {
			SimpleEntity entity1 = entityManager.find( SimpleEntity.class, id1 );
			SimpleEntity entity2 = entityManager.find( SimpleEntity.class, id2 );
			SimpleEntity entity3 = entityManager.find( SimpleEntity.class, id3 );

			entityManager.remove( entity1 );
			entityManager.remove( entity2 );
			entityManager.remove( entity3 );

			entityManager.flush();

			// All should be removed from persistence context
			assertFalse( entityManager.contains( entity1 ) );
			assertFalse( entityManager.contains( entity2 ) );
			assertFalse( entityManager.contains( entity3 ) );
		} );

		// Verify all are deleted
		scope.inTransaction( entityManager -> {
			assertNull( entityManager.find( SimpleEntity.class, id1 ) );
			assertNull( entityManager.find( SimpleEntity.class, id2 ) );
			assertNull( entityManager.find( SimpleEntity.class, id3 ) );
		} );
	}

	@Test
	public void testDeleteThenTryToFind(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Test";

			entityManager.persist( entity );
			entityManager.flush();

			Long entityId = entity.id;

			// Delete the entity
			entityManager.remove( entity );
			entityManager.flush();

			// Trying to find should return null (entity is deleted)
			SimpleEntity found = entityManager.find( SimpleEntity.class, entityId );
			assertNull( found, "Deleted entity should not be findable" );
		} );
	}

	@Test
	public void testDeleteMarksEntityAsNotManaged(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Test";

			entityManager.persist( entity );
			entityManager.flush();

			assertTrue( entityManager.contains( entity ), "Entity should be managed" );

			entityManager.remove( entity );
			entityManager.flush();

			assertFalse( entityManager.contains( entity ),
				"Entity should not be managed after delete" );

			// Accessing the entity is still possible (object still exists in memory)
			assertEquals( "Test", entity.name );
			assertNotNull( entity.id );

			// But it's not in the persistence context anymore
			SimpleEntity found = entityManager.find( SimpleEntity.class, entity.id );
			assertNull( found );
		} );
	}

	@Test
	public void testDeleteAfterUpdate(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long entityId = scope.fromTransaction( entityManager -> {
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Initial";

			entityManager.persist( entity );
			entityManager.flush();

			return entity.id;
		} );

		// Update then delete in same transaction
		scope.inTransaction( entityManager -> {
			SimpleEntity entity = entityManager.find( SimpleEntity.class, entityId );
			assertNotNull( entity );
			assertEquals( "Initial", entity.name );

			// Update
			entity.name = "Updated";
			entityManager.flush();

			// Delete
			entityManager.remove( entity );
			entityManager.flush();
		} );

		// Verify entity is deleted
		scope.inTransaction( entityManager -> {
			SimpleEntity entity = entityManager.find( SimpleEntity.class, entityId );
			assertNull( entity, "Entity should be deleted even after update" );
		} );
	}

	// Test entities

	@Entity(name = "SimpleEntity")
	@Table(name = "post_delete_simple")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;
	}

	@Entity(name = "EntityWithVersion")
	@Table(name = "post_delete_versioned")
	public static class EntityWithVersion {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@Version
		Long version;
	}

	@Entity(name = "EntityWithCallbacks")
	@Table(name = "post_delete_callbacks")
	public static class EntityWithCallbacks {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@Transient
		boolean preRemoveCalled = false;

		@Transient
		boolean postRemoveCalled = false;

		@PreRemove
		void preRemove() {
			preRemoveCalled = true;
		}

		@PostRemove
		void postRemove() {
			postRemoveCalled = true;
		}
	}
}
