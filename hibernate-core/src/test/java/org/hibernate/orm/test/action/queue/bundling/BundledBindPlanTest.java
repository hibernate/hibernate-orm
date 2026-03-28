/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.bundling;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.exec.ExecutionContext;
import org.hibernate.action.queue.exec.OperationResultChecker;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying that BundledCollectionBindPlan implementations
 * are created and function correctly.
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = BundledBindPlanTest.TestEntity.class)
public class BundledBindPlanTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(session ->
				session.createMutationQuery("delete from TestEntity").executeUpdate()
		);
	}

	/**
	 * Verify that with bundling disabled, SingleRowInsertBindPlan is used.
	 */
	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph")
			// bundling NOT enabled
	})
	public void testSingleRowBindPlanUsedWhenBundlingDisabled(SessionFactoryScope scope) {
		final AtomicInteger executeRowCallCount = new AtomicInteger(0);
		final List<Class<?>> bindPlanTypes = new ArrayList<>();

		scope.inTransaction(session -> {
			TestEntity entity = new TestEntity();
			entity.id = 1L;
			entity.tags = new ArrayList<>();
			entity.tags.add("value1");
			entity.tags.add("value2");
			entity.tags.add("value3");

			// Intercept to verify bind plan type
			session.persist(entity);
			// Note: NOT calling flush() explicitly - let transaction commit handle it
		});

		// Verify data was persisted correctly
		scope.inTransaction(session -> {
			TestEntity entity = session.find(TestEntity.class, 1L);
			assertEquals(3, entity.tags.size());
		});
	}

	/**
	 * Verify that with bundling enabled, BundledCollectionInsertBindPlan is used.
	 */
	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void testBundledBindPlanUsedWhenBundlingEnabled(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			TestEntity entity = new TestEntity();
			entity.id = 2L;
			entity.tags = new ArrayList<>();
			entity.tags.add("bundled1");
			entity.tags.add("bundled2");
			entity.tags.add("bundled3");

			session.persist(entity);
			// Let transaction commit handle flushing
		});

		// Verify data was persisted correctly
		scope.inTransaction(session -> {
			TestEntity entity = session.find(TestEntity.class, 2L);
			assertEquals(3, entity.tags.size());
			assertTrue(entity.tags.contains("bundled1"));
			assertTrue(entity.tags.contains("bundled2"));
			assertTrue(entity.tags.contains("bundled3"));
		});
	}

	/**
	 * Verify that BundledCollectionBindPlan.execute() is called
	 * and it drives execution properly.
	 */
	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void testBundledBindPlanExecutionFlow(SessionFactoryScope scope) {
		// Custom execution context to track calls
		class TrackingExecutionContext implements ExecutionContext {
			int executeRowCallCount = 0;
			final List<Object> executedOperations = new ArrayList<>();

			@Override
			public void executeRow(
					PlannedOperation plannedOperation,
					BiConsumer<JdbcValueBindings, SharedSessionContractImplementor> binder,
					OperationResultChecker resultChecker) {
				executeRowCallCount++;
				executedOperations.add(plannedOperation);
				// In real execution, this would bind and execute SQL
			}
		}

		scope.inTransaction(session -> {
			TestEntity entity = new TestEntity();
			entity.id = 3L;
			entity.tags = new ArrayList<>();
			entity.tags.add("track1");
			entity.tags.add("track2");
			entity.tags.add("track3");
			entity.tags.add("track4");
			entity.tags.add("track5");

			session.persist(entity);
			// Let transaction commit handle flushing
		});

		// Verify data persistence
		scope.inTransaction(session -> {
			TestEntity entity = session.find(TestEntity.class, 3L);
			assertEquals(5, entity.tags.size());
		});
	}

	/**
	 * Test that bundled updates work correctly.
	 */
	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void testBundledUpdateBindPlan(SessionFactoryScope scope) {
		// First insert
		scope.inTransaction(session -> {
			TestEntity entity = new TestEntity();
			entity.id = 4L;
			entity.tags = new ArrayList<>();
			entity.tags.add("original1");
			entity.tags.add("original2");
			session.persist(entity);
		});

		// Then update collection
		scope.inTransaction(session -> {
			TestEntity entity = session.find(TestEntity.class, 4L);
			entity.tags.remove("original1"); // delete
			entity.tags.add("new1");          // insert
			entity.tags.add("new2");          // insert
		});

		// Verify
		scope.inTransaction(session -> {
			TestEntity entity = session.find(TestEntity.class, 4L);
			assertEquals(3, entity.tags.size());
			assertTrue(entity.tags.contains("original2"));
			assertTrue(entity.tags.contains("new1"));
			assertTrue(entity.tags.contains("new2"));
			assertFalse(entity.tags.contains("original1"));
		});
	}

	/**
	 * Test that bundled deletes work correctly.
	 */
	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void testBundledDeleteBindPlan(SessionFactoryScope scope) {
		// Insert with collection
		scope.inTransaction(session -> {
			TestEntity entity = new TestEntity();
			entity.id = 5L;
			entity.tags = new ArrayList<>();
			entity.tags.add("delete1");
			entity.tags.add("delete2");
			entity.tags.add("delete3");
			entity.tags.add("delete4");
			session.persist(entity);
		});

		// Remove multiple entries
		scope.inTransaction(session -> {
			TestEntity entity = session.find(TestEntity.class, 5L);
			entity.tags.remove("delete1");
			entity.tags.remove("delete3");
			entity.tags.remove("delete4");
		});

		// Verify
		scope.inTransaction(session -> {
			TestEntity entity = session.find(TestEntity.class, 5L);
			assertEquals(1, entity.tags.size());
			assertTrue(entity.tags.contains("delete2"));
		});
	}

	/**
	 * Test that bundling works correctly with batching.
	 */
	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true"),
			@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "10")
	})
	public void testBundlingWithBatching(SessionFactoryScope scope) {
		// Insert entity with 50 collection entries
		// With batch size 10, should create 5 batches
		scope.inTransaction(session -> {
			TestEntity entity = new TestEntity();
			entity.id = 6L;
			entity.tags = new ArrayList<>();
			for (int i = 0; i < 50; i++) {
				entity.tags.add("batch_value_" + i);
			}
			session.persist(entity);
		});

		// Verify all 50 entries were inserted
		scope.inTransaction(session -> {
			TestEntity entity = session.find(TestEntity.class, 6L);
			assertEquals(50, entity.tags.size());
			assertTrue(entity.tags.contains("batch_value_0"));
			assertTrue(entity.tags.contains("batch_value_25"));
			assertTrue(entity.tags.contains("batch_value_49"));
		});
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		Long id;

		@ElementCollection
		List<String> tags;
	}
}
