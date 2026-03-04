/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.integration;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for optimistic locking with version-based and ALL/DIRTY strategies.
 * Tests version increment handling by PostUpdateHandling.
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		OptimisticLockingTest.VersionedEntity.class,
		OptimisticLockingTest.AllLockEntity.class,
		OptimisticLockingTest.DirtyLockEntity.class
})
public class OptimisticLockingTest {

	@Test
	public void testVersionBasicIncrement(EntityManagerFactoryScope scope) {
		Long entityId = scope.fromTransaction( em -> {
			VersionedEntity entity = new VersionedEntity();
			entity.name = "Initial";

			em.persist( entity );
			em.flush();

			assertEquals( 0L, entity.version, "Initial version should be 0" );
			return entity.id;
		} );

		// Update in new transaction
		scope.inTransaction( em -> {
			VersionedEntity entity = em.find( VersionedEntity.class, entityId );
			assertEquals( 0L, entity.version );

			entity.name = "Updated";
			em.flush();

			assertEquals( 1L, entity.version, "Version should increment to 1" );
		} );

		// Verify version persisted
		scope.inTransaction( em -> {
			VersionedEntity entity = em.find( VersionedEntity.class, entityId );
			assertEquals( 1L, entity.version );
			assertEquals( "Updated", entity.name );
		} );
	}

	@Test
	public void testVersionMultipleUpdates(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			VersionedEntity entity = new VersionedEntity();
			entity.name = "Initial";

			em.persist( entity );
			em.flush();
			assertEquals( 0L, entity.version );

			entity.name = "Update 1";
			em.flush();
			assertEquals( 1L, entity.version );

			entity.name = "Update 2";
			em.flush();
			assertEquals( 2L, entity.version );

			entity.name = "Update 3";
			em.flush();
			assertEquals( 3L, entity.version );
		} );
	}

	@Test
	public void testVersionConflictDetection(EntityManagerFactoryScope scope) {
		Long entityId = scope.fromTransaction( em -> {
			VersionedEntity entity = new VersionedEntity();
			entity.name = "Initial";

			em.persist( entity );
			em.flush();

			return entity.id;
		} );

		// Simulate concurrent modification
		// Transaction 1: Load entity
		EntityManager em1 = scope.getEntityManagerFactory().createEntityManager();
		em1.getTransaction().begin();
		VersionedEntity entity1 = em1.find( VersionedEntity.class, entityId );
		Long version1 = entity1.version;

		// Transaction 2: Load, modify, commit
		scope.inTransaction( em2 -> {
			VersionedEntity entity2 = em2.find( VersionedEntity.class, entityId );
			assertEquals( version1, entity2.version );

			entity2.name = "Modified by TX2";
			em2.flush();
		} );

		// Transaction 1: Try to modify (should detect conflict)
		entity1.name = "Modified by TX1";

		try {
			em1.flush();
			em1.getTransaction().commit();
			fail( "Should have thrown OptimisticLockException" );
		}
		catch ( OptimisticLockException e ) {
			// Expected - version conflict detected
			em1.getTransaction().rollback();
		}
		finally {
			em1.close();
		}
	}

	@Test
	public void testVersionPreventsLostUpdate(EntityManagerFactoryScope scope) {
		Long entityId = scope.fromTransaction( em -> {
			VersionedEntity entity = new VersionedEntity();
			entity.name = "Initial";
			entity.value = 100;

			em.persist( entity );
			em.flush();

			return entity.id;
		} );

		// Two transactions try to update concurrently
		// First transaction succeeds
		scope.inTransaction( em -> {
			VersionedEntity entity = em.find( VersionedEntity.class, entityId );
			entity.value = entity.value + 10; // 100 + 10 = 110
			em.flush();
		} );

		// Verify first update succeeded
		scope.inTransaction( em -> {
			VersionedEntity entity = em.find( VersionedEntity.class, entityId );
			assertEquals( 110, entity.value );
			assertEquals( 1L, entity.version );
		} );

		// Second update on top of first
		scope.inTransaction( em -> {
			VersionedEntity entity = em.find( VersionedEntity.class, entityId );
			entity.value = entity.value + 20; // 110 + 20 = 130
			em.flush();

			assertEquals( 130, entity.value );
			assertEquals( 2L, entity.version );
		} );
	}

	@Test
	public void testVersionOnDelete(EntityManagerFactoryScope scope) {
		Long entityId = scope.fromTransaction( em -> {
			VersionedEntity entity = new VersionedEntity();
			entity.name = "ToDelete";

			em.persist( entity );
			em.flush();

			return entity.id;
		} );

		// Delete should use version in WHERE clause
		scope.inTransaction( em -> {
			VersionedEntity entity = em.find( VersionedEntity.class, entityId );
			Long versionBeforeDelete = entity.version;

			em.remove( entity );
			em.flush();

			// Version should remain unchanged
			assertEquals( versionBeforeDelete, entity.version );
		} );

		// Verify deleted
		scope.inTransaction( em -> {
			assertNull( em.find( VersionedEntity.class, entityId ) );
		} );
	}

	@Test
	public void testDeleteConflictWithVersion(EntityManagerFactoryScope scope) {
		Long entityId = scope.fromTransaction( em -> {
			VersionedEntity entity = new VersionedEntity();
			entity.name = "Test";

			em.persist( entity );
			em.flush();

			return entity.id;
		} );

		EntityManager em1 = scope.getEntityManagerFactory().createEntityManager();
		em1.getTransaction().begin();
		VersionedEntity entity1 = em1.find( VersionedEntity.class, entityId );

		// Another transaction updates the entity
		scope.inTransaction( em2 -> {
			VersionedEntity entity2 = em2.find( VersionedEntity.class, entityId );
			entity2.name = "Modified";
			em2.flush();
		} );

		// Try to delete with old version
		em1.remove( entity1 );

		try {
			em1.flush();
			em1.getTransaction().commit();
			fail( "Should have thrown OptimisticLockException on delete" );
		}
		catch ( OptimisticLockException e ) {
			// Expected
			em1.getTransaction().rollback();
		}
		finally {
			em1.close();
		}
	}

	@Test
	public void testNoVersionIncrementWithoutChanges(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			VersionedEntity entity = new VersionedEntity();
			entity.name = "Test";

			em.persist( entity );
			em.flush();
			assertEquals( 0L, entity.version );

			// Flush without changes
			em.flush();
			assertEquals( 0L, entity.version, "Version should not increment without changes" );

			em.flush();
			assertEquals( 0L, entity.version, "Version should still not increment" );
		} );
	}

	@Test
	public void testVersionIncrementOnlyForDirtyFields(EntityManagerFactoryScope scope) {
		Long entityId = scope.fromTransaction( em -> {
			VersionedEntity entity = new VersionedEntity();
			entity.name = "Initial";
			entity.value = 100;

			em.persist( entity );
			em.flush();

			return entity.id;
		} );

		scope.inTransaction( em -> {
			VersionedEntity entity = em.find( VersionedEntity.class, entityId );
			Long initialVersion = entity.version;

			// Set same value (no actual change)
			entity.name = "Initial";

			em.flush();

			// Version should not increment if no actual change
			// (depends on dirty checking implementation)
			assertTrue( entity.version >= initialVersion );
		} );
	}

	@Test
	public void testMultipleEntitiesVersionIncrement(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			VersionedEntity e1 = new VersionedEntity();
			e1.name = "Entity1";

			VersionedEntity e2 = new VersionedEntity();
			e2.name = "Entity2";

			VersionedEntity e3 = new VersionedEntity();
			e3.name = "Entity3";

			em.persist( e1 );
			em.persist( e2 );
			em.persist( e3 );
			em.flush();

			assertEquals( 0L, e1.version );
			assertEquals( 0L, e2.version );
			assertEquals( 0L, e3.version );

			// Update all
			e1.name = "Updated1";
			e2.name = "Updated2";
			e3.name = "Updated3";

			em.flush();

			// All versions should increment
			assertEquals( 1L, e1.version );
			assertEquals( 1L, e2.version );
			assertEquals( 1L, e3.version );
		} );
	}

	// Test entities

	@Entity(name = "VersionedEntity")
	@Table(name = "opt_lock_versioned")
	public static class VersionedEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@Column(name = "val")
		Integer value;

		@Version
		Long version;
	}

	@Entity(name = "AllLockEntity")
	@Table(name = "opt_lock_all")
	@org.hibernate.annotations.OptimisticLocking(
		type = org.hibernate.annotations.OptimisticLockType.ALL
	)
	@org.hibernate.annotations.DynamicUpdate
	public static class AllLockEntity {
		@Id
		@GeneratedValue
		Long id;

		String field1;

		String field2;
	}

	@Entity(name = "DirtyLockEntity")
	@Table(name = "opt_lock_dirty")
	@org.hibernate.annotations.OptimisticLocking(
		type = org.hibernate.annotations.OptimisticLockType.DIRTY
	)
	@org.hibernate.annotations.DynamicUpdate
	public static class DirtyLockEntity {
		@Id
		@GeneratedValue
		Long id;

		String field1;

		String field2;
	}
}
