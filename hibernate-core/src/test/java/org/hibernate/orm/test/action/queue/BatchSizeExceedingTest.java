/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue;

import jakarta.persistence.*;
import org.hibernate.action.queue.QueueType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ActionQueue behavior when operations exceed the configured batch size.
 *
 * These tests verify that both graph-based and legacy ActionQueue implementations
 * correctly handle scenarios where the number of pending actions exceeds the
 * JDBC batch size setting, requiring multiple batch flushes.
 *
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				BatchSizeExceedingTest.TestEntity.class,
				BatchSizeExceedingTest.Parent.class,
				BatchSizeExceedingTest.Child.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "20")
		}
)
@SessionFactory
public class BatchSizeExceedingTest {

	@Entity(name = "TestEntity")
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@Column(name = "entity_value")
		private int value;

		public TestEntity() {}

		public TestEntity(String name, int value) {
			this.name = name;
			this.value = value;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
	}

	@Entity(name = "Parent")
	@Table(name = "batch_parent")
	public static class Parent {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Child> children = new ArrayList<>();

		public Parent() {}

		public Parent(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void addChild(Child child) {
			children.add(child);
			child.parent = this;
		}

		public List<Child> getChildren() {
			return children;
		}
	}

	@Entity(name = "Child")
	@Table(name = "batch_child")
	public static class Child {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn(name = "parent_id")
		private Parent parent;

		public Child() {}

		public Child(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.createMutationQuery("delete from Child").executeUpdate();
			session.createMutationQuery("delete from Parent").executeUpdate();
			session.createMutationQuery("delete from TestEntity").executeUpdate();
		});
	}

	@Test
	public void testInsertExceedingBatchSize_50Entities(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Batch size is 20, insert 50 entities (requires 3 batches)
		scope.inTransaction(session -> {
			for (int i = 0; i < 50; i++) {
				session.persist(new TestEntity("Entity-" + i, i));
			}
		});

		// Verify all entities were persisted
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(e) from TestEntity e", Long.class)
					.getSingleResult();
			assertEquals(50, count, "All 50 entities should be persisted");
		});
	}

	@Test
	public void testInsertExceedingBatchSize_100Entities(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Batch size is 20, insert 100 entities (requires 5 batches)
		scope.inTransaction(session -> {
			for (int i = 0; i < 100; i++) {
				session.persist(new TestEntity("Entity-" + i, i));
			}
		});

		// Verify all entities were persisted
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(e) from TestEntity e", Long.class)
					.getSingleResult();
			assertEquals(100, count, "All 100 entities should be persisted");
		});
	}

	@Test
	public void testUpdateExceedingBatchSize(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Insert 50 entities
		List<Long> ids = new ArrayList<>();
		scope.inTransaction(session -> {
			for (int i = 0; i < 50; i++) {
				TestEntity entity = new TestEntity("Entity-" + i, i);
				session.persist(entity);
				session.flush(); // Ensure ID is generated
				ids.add(entity.getId());
			}
		});

		// Update all 50 entities (exceeds batch size of 20)
		scope.inTransaction(session -> {
			for (Long id : ids) {
				TestEntity entity = session.find(TestEntity.class, id);
				entity.setValue(entity.getValue() * 2);
			}
		});

		// Verify all updates were applied
		scope.inTransaction(session -> {
			for (int i = 0; i < ids.size(); i++) {
				TestEntity entity = session.find(TestEntity.class, ids.get(i));
				assertEquals(i * 2, entity.getValue(), "Entity " + i + " should have doubled value");
			}
		});
	}

	@Test
	public void testDeleteExceedingBatchSize(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Insert 60 entities
		List<Long> ids = new ArrayList<>();
		scope.inTransaction(session -> {
			for (int i = 0; i < 60; i++) {
				TestEntity entity = new TestEntity("Entity-" + i, i);
				session.persist(entity);
				session.flush();
				ids.add(entity.getId());
			}
		});

		// Delete all 60 entities (exceeds batch size of 20, requires 3 batches)
		scope.inTransaction(session -> {
			for (Long id : ids) {
				TestEntity entity = session.find(TestEntity.class, id);
				session.remove(entity);
			}
		});

		// Verify all entities were deleted
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(e) from TestEntity e", Long.class)
					.getSingleResult();
			assertEquals(0, count, "All entities should be deleted");
		});
	}

	@Test
	public void testMixedOperationsExceedingBatchSize(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Insert 30 entities
		List<Long> ids = new ArrayList<>();
		scope.inTransaction(session -> {
			for (int i = 0; i < 30; i++) {
				TestEntity entity = new TestEntity("Entity-" + i, i);
				session.persist(entity);
				session.flush();
				ids.add(entity.getId());
			}
		});

		// In single transaction: update 25, delete 10, insert 15 new
		// Total: 50 operations across 3 batches
		scope.inTransaction(session -> {
			// Update first 25
			for (int i = 0; i < 25; i++) {
				TestEntity entity = session.find(TestEntity.class, ids.get(i));
				entity.setName("Updated-" + i);
			}

			// Delete last 10
			for (int i = 20; i < 30; i++) {
				TestEntity entity = session.find(TestEntity.class, ids.get(i));
				session.remove(entity);
			}

			// Insert 15 new
			for (int i = 0; i < 15; i++) {
				session.persist(new TestEntity("New-" + i, 100 + i));
			}
		});

		// Verify final state: 20 original + 15 new = 35 total
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(e) from TestEntity e", Long.class)
					.getSingleResult();
			assertEquals(35, count, "Should have 35 entities (20 original + 15 new)");

			// Verify updates were applied
			long updatedCount = session.createQuery(
					"select count(e) from TestEntity e where e.name like 'Updated-%'", Long.class)
					.getSingleResult();
			assertEquals(20, updatedCount, "Should have 20 updated entities");
		});
	}

	@Test
	public void testCascadeExceedingBatchSize(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Create 25 parents with 3 children each = 100 entities total
		// With batch size 20, this requires multiple batches
		scope.inTransaction(session -> {
			for (int i = 0; i < 25; i++) {
				Parent parent = new Parent("Parent-" + i);
				for (int j = 0; j < 3; j++) {
					parent.addChild(new Child("Child-" + i + "-" + j));
				}
				session.persist(parent);
			}
		});

		// Verify all entities were persisted
		scope.inTransaction(session -> {
			long parentCount = session.createQuery("select count(p) from Parent p", Long.class)
					.getSingleResult();
			assertEquals(25, parentCount, "All 25 parents should be persisted");

			long childCount = session.createQuery("select count(c) from Child c", Long.class)
					.getSingleResult();
			assertEquals(75, childCount, "All 75 children should be persisted");
		});
	}

	@Test
	public void testLargeCascadeExceedingBatchSize(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Create 10 parents with 15 children each = 160 entities total
		// This tests large cascades spanning multiple batches
		scope.inTransaction(session -> {
			for (int i = 0; i < 10; i++) {
				Parent parent = new Parent("Parent-" + i);
				for (int j = 0; j < 15; j++) {
					parent.addChild(new Child("Child-" + i + "-" + j));
				}
				session.persist(parent);
			}
		});

		// Verify correct counts
		scope.inTransaction(session -> {
			long parentCount = session.createQuery("select count(p) from Parent p", Long.class)
					.getSingleResult();
			assertEquals(10, parentCount);

			long childCount = session.createQuery("select count(c) from Child c", Long.class)
					.getSingleResult();
			assertEquals(150, childCount);

			// Verify FK relationships are correct
			for (int i = 0; i < 10; i++) {
				long childrenForParent = session.createQuery(
						"select count(c) from Child c where c.parent.name = :name", Long.class)
						.setParameter("name", "Parent-" + i)
						.getSingleResult();
				assertEquals(15, childrenForParent, "Parent " + i + " should have 15 children");
			}
		});
	}

	@Test
	public void testCascadeDeleteExceedingBatchSize(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Create and persist parents with children
		List<Long> parentIds = new ArrayList<>();
		scope.inTransaction(session -> {
			for (int i = 0; i < 20; i++) {
				Parent parent = new Parent("Parent-" + i);
				for (int j = 0; j < 4; j++) {
					parent.addChild(new Child("Child-" + i + "-" + j));
				}
				session.persist(parent);
				session.flush();
				parentIds.add(parent.getId());
			}
		});

		// Delete all parents (cascade deletes children)
		// 20 parents + 80 children = 100 delete operations
		scope.inTransaction(session -> {
			for (Long parentId : parentIds) {
				Parent parent = session.find(Parent.class, parentId);
				session.remove(parent);
			}
		});

		// Verify all were deleted
		scope.inTransaction(session -> {
			long parentCount = session.createQuery("select count(p) from Parent p", Long.class)
					.getSingleResult();
			assertEquals(0, parentCount, "All parents should be deleted");

			long childCount = session.createQuery("select count(c) from Child c", Long.class)
					.getSingleResult();
			assertEquals(0, childCount, "All children should be cascade-deleted");
		});
	}

	@Test
	public void testExtremelyLargeBatch_1000Entities(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Test with 1000 entities (50 batches with batch size 20)
		// This tests ActionQueue scalability
		scope.inTransaction(session -> {
			for (int i = 0; i < 1000; i++) {
				session.persist(new TestEntity("Entity-" + i, i));
			}
		});

		// Verify count
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(e) from TestEntity e", Long.class)
					.getSingleResult();
			assertEquals(1000, count, "All 1000 entities should be persisted");
		});
	}
}
