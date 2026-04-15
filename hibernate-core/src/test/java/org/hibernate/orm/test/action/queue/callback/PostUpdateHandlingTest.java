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
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PostUpdateHandling post-execution callback.
 * Verifies that all finalization work is properly executed after UPDATE operations.
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		PostUpdateHandlingTest.SimpleEntity.class,
		PostUpdateHandlingTest.EntityWithVersion.class,
		PostUpdateHandlingTest.EntityWithCallbacks.class
})
public class PostUpdateHandlingTest {

	@Test
	public void testApplicationGeneratedVersionIncrement(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			EntityWithVersion entity = new EntityWithVersion();
			entity.name = "Initial";

			entityManager.persist( entity );
			entityManager.flush();

			Long initialVersion = entity.version;
			assertNotNull( initialVersion, "Version should be set after insert" );
			assertEquals( 0L, initialVersion, "Initial version should be 0" );

			// Modify entity
			entity.name = "Updated";
			entityManager.flush();

			// PostUpdateHandling should have incremented the version
			assertNotNull( entity.version );
			assertEquals( initialVersion + 1, entity.version,
				"Version should be incremented after update" );
		} );
	}

	@Test
	public void testMultipleUpdatesIncrementVersion(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			EntityWithVersion entity = new EntityWithVersion();
			entity.name = "Initial";

			entityManager.persist( entity );
			entityManager.flush();

			Long version0 = entity.version;

			// First update
			entity.name = "Update1";
			entityManager.flush();
			Long version1 = entity.version;
			assertEquals( version0 + 1, version1 );

			// Second update
			entity.name = "Update2";
			entityManager.flush();
			Long version2 = entity.version;
			assertEquals( version1 + 1, version2 );

			// Third update
			entity.name = "Update3";
			entityManager.flush();
			Long version3 = entity.version;
			assertEquals( version2 + 1, version3 );
		} );
	}

	@Test
	public void testPostUpdateCallbackFired(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			EntityWithCallbacks entity = new EntityWithCallbacks();
			entity.name = "Initial";

			entityManager.persist( entity );
			entityManager.flush();

			// Reset flags
			entity.preUpdateCalled = false;
			entity.postUpdateCalled = false;

			// Modify entity
			entity.name = "Updated";

			// Callbacks should not have fired yet
			assertFalse( entity.preUpdateCalled );
			assertFalse( entity.postUpdateCalled );

			entityManager.flush();

			// PostUpdateHandling should fire PostUpdate event
			assertTrue( entity.preUpdateCalled, "PreUpdate should be called before flush" );
			assertTrue( entity.postUpdateCalled, "PostUpdate should be called after flush" );
		} );
	}

	@Test
	public void testUpdateWithNoChanges(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			EntityWithVersion entity = new EntityWithVersion();
			entity.name = "Initial";

			entityManager.persist( entity );
			entityManager.flush();

			Long initialVersion = entity.version;

			// Flush without making changes
			entityManager.flush();

			// Version should NOT be incremented (no update executed)
			assertEquals( initialVersion, entity.version,
				"Version should not change when no fields are modified" );
		} );
	}

	@Test
	public void testEntityStateAfterUpdate(EntityManagerFactoryScope scope) {
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

		// Update in new transaction
		scope.inTransaction( entityManager -> {
			SimpleEntity entity = entityManager.find( SimpleEntity.class, entityId );
			assertEquals( "Initial", entity.name );

			entity.name = "Updated";
			entityManager.flush();

			// Verify state is updated in same session
			assertEquals( "Updated", entity.name );
		} );

		// Verify state persists across transactions
		scope.inTransaction( entityManager -> {
			SimpleEntity entity = entityManager.find( SimpleEntity.class, entityId );
			assertEquals( "Updated", entity.name, "Update should persist across transactions" );
		} );
	}

	@Test
	public void testVersionUsedForOptimisticLocking(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long entityId = scope.fromTransaction( entityManager -> {
			EntityWithVersion entity = new EntityWithVersion();
			entity.name = "Initial";

			entityManager.persist( entity );
			entityManager.flush();

			return entity.id;
		} );

		// Simulate concurrent modification in separate transaction
		scope.inTransaction( entityManager -> {
			EntityWithVersion entity = entityManager.find( EntityWithVersion.class, entityId );
			entity.name = "Modified by TX1";
			entityManager.flush();
		} );

		// This should work - version was updated correctly
		scope.inTransaction( entityManager -> {
			EntityWithVersion entity = entityManager.find( EntityWithVersion.class, entityId );
			assertEquals( "Modified by TX1", entity.name );
			assertEquals( 1L, entity.version, "Version should be 1 after first update" );

			entity.name = "Modified by TX2";
			entityManager.flush();

			assertEquals( 2L, entity.version, "Version should be 2 after second update" );
		} );
	}

	@Test
	public void testUpdateThenFindInSameTransaction(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Initial";

			entityManager.persist( entity );
			entityManager.flush();

			Long entityId = entity.id;

			entity.name = "Updated";
			entityManager.flush();

			// Clear persistence context
			entityManager.clear();

			// Re-load entity
			SimpleEntity reloaded = entityManager.find( SimpleEntity.class, entityId );
			assertEquals( "Updated", reloaded.name,
				"Updated value should be persisted to database" );
		} );
	}

	@Test
	public void testMultipleUpdatesInSameFlush(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			SimpleEntity entity1 = new SimpleEntity();
			entity1.name = "Entity1";

			SimpleEntity entity2 = new SimpleEntity();
			entity2.name = "Entity2";

			entityManager.persist( entity1 );
			entityManager.persist( entity2 );
			entityManager.flush();

			// Modify both
			entity1.name = "Entity1 Updated";
			entity2.name = "Entity2 Updated";

			entityManager.flush();

			// Both should be updated
			assertEquals( "Entity1 Updated", entity1.name );
			assertEquals( "Entity2 Updated", entity2.name );

			// Verify in database
			entityManager.clear();

			SimpleEntity reloaded1 = entityManager.find( SimpleEntity.class, entity1.id );
			SimpleEntity reloaded2 = entityManager.find( SimpleEntity.class, entity2.id );

			assertEquals( "Entity1 Updated", reloaded1.name );
			assertEquals( "Entity2 Updated", reloaded2.name );
		} );
	}

	// Test entities

	@Entity(name = "SimpleEntity")
	@Table(name = "post_update_simple")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;
	}

	@Entity(name = "EntityWithVersion")
	@Table(name = "post_update_versioned")
	public static class EntityWithVersion {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@Version
		Long version;
	}

	@Entity(name = "EntityWithCallbacks")
	@Table(name = "post_update_callbacks")
	public static class EntityWithCallbacks {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@Transient
		boolean preUpdateCalled = false;

		@Transient
		boolean postUpdateCalled = false;

		@PreUpdate
		void preUpdate() {
			preUpdateCalled = true;
		}

		@PostUpdate
		void postUpdate() {
			postUpdateCalled = true;
		}
	}
}
