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
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PostInsertHandling post-execution callback.
 * Verifies that all finalization work is properly executed after INSERT operations.
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		PostInsertHandlingTest.SimpleEntity.class,
		PostInsertHandlingTest.EntityWithGeneratedId.class,
		PostInsertHandlingTest.EntityWithCallbacks.class,
		PostInsertHandlingTest.CacheableEntity.class
})
public class PostInsertHandlingTest {

	@Test
	public void testProcessGeneratedId(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			EntityWithGeneratedId entity = new EntityWithGeneratedId();
			entity.name = "Test";

			// ID should be null before persist
			assertNull( entity.id );

			entityManager.persist( entity );
			entityManager.flush();

			// PostInsertHandling should have set the generated ID
			assertNotNull( entity.id, "Generated ID should be set after flush" );
			assertTrue( entity.id > 0, "Generated ID should be positive" );
		} );
	}

	@Test
	public void testEntityRegisteredInPersistenceContext(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Test";

			entityManager.persist( entity );
			entityManager.flush();

			// Entity should be managed (in persistence context)
			assertTrue( entityManager.contains( entity ),
				"Entity should be managed after insert" );

			// Should be able to find by ID
			assertNotNull( entity.id );
			SimpleEntity found = entityManager.find( SimpleEntity.class, entity.id );
			assertSame( entity, found, "Should return same instance from persistence context" );
		} );
	}

	@Test
	public void testPostPersistCallbackFired(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			EntityWithCallbacks entity = new EntityWithCallbacks();
			entity.name = "Test";

			// Callbacks should not have fired yet
			assertFalse( entity.prePersistCalled );
			assertFalse( entity.postPersistCalled );

			entityManager.persist( entity );

			// PrePersist should fire immediately
			assertTrue( entity.prePersistCalled, "PrePersist should be called on persist()" );
			assertFalse( entity.postPersistCalled, "PostPersist should not be called yet" );

			entityManager.flush();

			// PostInsertHandling should fire PostPersist event
			assertTrue( entity.postPersistCalled, "PostPersist should be called after flush" );
			assertNotNull( entity.id, "ID should be generated before PostPersist" );
		} );
	}

	@Test
	public void testMultipleInsertsInSameTransaction(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
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

			// All entities should have IDs and be managed
			assertNotNull( entity1.id );
			assertNotNull( entity2.id );
			assertNotNull( entity3.id );

			assertTrue( entityManager.contains( entity1 ) );
			assertTrue( entityManager.contains( entity2 ) );
			assertTrue( entityManager.contains( entity3 ) );

			// IDs should be unique
			assertNotEquals( entity1.id, entity2.id );
			assertNotEquals( entity2.id, entity3.id );
			assertNotEquals( entity1.id, entity3.id );
		} );
	}

	@Test
	public void testInsertThenFind(EntityManagerFactoryScope scope) {
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

		// Verify entity was actually inserted and can be loaded in new session
		scope.inTransaction( entityManager -> {
			SimpleEntity found = entityManager.find( SimpleEntity.class, entityId );
			assertNotNull( found, "Entity should be loadable in new transaction" );
			assertEquals( "Test", found.name );
		} );
	}

	@Test
	public void testPersistWithPreAssignedId(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( entityManager -> {
			EntityWithGeneratedId entity = new EntityWithGeneratedId();
			entity.name = "Test";

			entityManager.persist( entity );
			entityManager.flush();

			Long generatedId = entity.id;
			assertNotNull( generatedId );

			// ID should persist across transactions
			entityManager.clear();

			EntityWithGeneratedId reloaded = entityManager.find(
				EntityWithGeneratedId.class, generatedId );
			assertNotNull( reloaded );
			assertEquals( "Test", reloaded.name );
			assertEquals( generatedId, reloaded.id );
		} );
	}

	// Test entities

	@Entity(name = "SimpleEntity")
	@Table(name = "post_insert_simple")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;
	}

	@Entity(name = "EntityWithGeneratedId")
	@Table(name = "post_insert_generated")
	public static class EntityWithGeneratedId {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		String name;
	}

	@Entity(name = "EntityWithCallbacks")
	@Table(name = "post_insert_callbacks")
	public static class EntityWithCallbacks {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@Transient
		boolean prePersistCalled = false;

		@Transient
		boolean postPersistCalled = false;

		@PrePersist
		void prePersist() {
			prePersistCalled = true;
		}

		@PostPersist
		void postPersist() {
			postPersistCalled = true;
		}
	}

	@Entity(name = "CacheableEntity")
	@Table(name = "post_insert_cacheable")
	@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
	public static class CacheableEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;
	}
}
