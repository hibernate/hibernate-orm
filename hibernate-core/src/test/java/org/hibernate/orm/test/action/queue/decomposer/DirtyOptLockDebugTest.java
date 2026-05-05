/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.decomposer;

import org.hibernate.action.queue.QueueType;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Debug test for DIRTY optimistic lock issue
 */
@DomainModel(annotatedClasses = DirtyOptLockDebugTest.TestEntity.class)
@SessionFactory
public class DirtyOptLockDebugTest {

	@Test
	public void testDirtyOptimisticLock(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long id = scope.fromTransaction( session -> {
			TestEntity entity = new TestEntity();
			entity.field1 = "Field1";
			entity.field2 = "Field2";
			session.persist( entity );
			System.out.println( "After persist - field1: " + entity.field1 + ", field2: " + entity.field2 );
			return entity.id;
		} );

		// Verify what's in the database
		scope.inTransaction( session -> {
			TestEntity entity = session.find( TestEntity.class, id );
			System.out.println( "After load - field1: " + entity.field1 + ", field2: " + entity.field2 );

			// Get the loaded state from EntityEntry
			var entry = session.getPersistenceContext().getEntry( entity );
			Object[] loadedState = entry.getLoadedState();
			System.out.println( "LoadedState[0] (field1): " + loadedState[0] );
			System.out.println( "LoadedState[1] (field2): " + loadedState[1] );

			// Now modify field1
			entity.field1 = "Field1 Updated";
			System.out.println( "After modify - field1: " + entity.field1 + ", field2: " + entity.field2 );
			System.out.println( "LoadedState[0] (field1): " + loadedState[0] );
			System.out.println( "LoadedState[1] (field2): " + loadedState[1] );

			// NOW FLUSH - this is where the failure should occur
			System.out.println( "About to flush..." );
			try {
				session.flush();
				System.out.println( "Flush completed successfully!" );
			}
			catch ( Exception e ) {
				System.out.println( "Flush FAILED with: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
				throw e;
			}
		} );
	}

	@Entity(name = "TestEntity")
	@Table(name = "test_dirty_opt_lock")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	public static class TestEntity {
		@Id
		@GeneratedValue
		Long id;

		String field1;
		String field2;
	}
}
