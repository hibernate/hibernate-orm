/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import jakarta.persistence.*;
import org.hibernate.cfg.FlushSettings;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance comparison between legacy ActionQueue and graph-based ActionQueue.
 *
 * Run scenarios:
 * 1. Simple inserts (no FKs) - baseline
 * 2. Parent-child inserts (single FK)
 * 3. Complex graph (multiple FKs)
 * 4. Self-referencing FKs
 * 5. Circular dependencies
 */
public class ActionQueuePerformanceTest {

	@Entity(name = "SimpleEntity")
	@Table(name = "simple_entity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String name;

		@Column(name = "data_value")
		private int value;

		public SimpleEntity() {}
		public SimpleEntity(String name, int value) {
			this.name = name;
			this.value = value;
		}
	}

	@Entity(name = "Parent")
	@Table(name = "parent")
	public static class Parent {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		private List<Child> children = new ArrayList<>();

		public Parent() {}
		public Parent(String name) {
			this.name = name;
		}

		public void addChild(Child child) {
			children.add(child);
			child.parent = this;
		}
	}

	@Entity(name = "Child")
	@Table(name = "child")
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
	}

	@Entity(name = "Node")
	@Table(name = "node")
	public static class Node {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String name;

		@ManyToOne
		@JoinColumn(name = "parent_id")
		private Node parent;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		private List<Node> children = new ArrayList<>();

		public Node() {}
		public Node(String name) {
			this.name = name;
		}

		public void addChild(Node child) {
			children.add(child);
			child.parent = this;
		}
	}

	/**
	 * Test simple inserts with no FK dependencies
	 */
	@DomainModel(annotatedClasses = SimpleEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
	public static class SimpleLegacyTest {
		@Test
		public void testSimpleInserts(SessionFactoryScope scope) {
			runSimpleInsertTest(scope, "LEGACY", 1000);
		}
	}

	@DomainModel(annotatedClasses = SimpleEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"))
	public static class SimpleGraphTest {
		@Test
		public void testSimpleInserts(SessionFactoryScope scope) {
			runSimpleInsertTest(scope, "GRAPH", 1000);
		}
	}

	/**
	 * Test parent-child inserts with FK dependencies
	 */
	@DomainModel(annotatedClasses = {Parent.class, Child.class})
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
	public static class ParentChildLegacyTest {
		@Test
		public void testParentChildInserts(SessionFactoryScope scope) {
			runParentChildTest(scope, "LEGACY", 100);
		}
	}

	@DomainModel(annotatedClasses = {Parent.class, Child.class})
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"))
	public static class ParentChildGraphTest {
		@Test
		public void testParentChildInserts(SessionFactoryScope scope) {
			runParentChildTest(scope, "GRAPH", 100);
		}
	}

	/**
	 * Test self-referencing FKs (tree structure)
	 */
	@DomainModel(annotatedClasses = Node.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
	public static class SelfRefLegacyTest {
		@Test
		public void testSelfReferencingInserts(SessionFactoryScope scope) {
			runSelfReferencingTest(scope, "LEGACY", 50);
		}
	}

	@DomainModel(annotatedClasses = Node.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"))
	public static class SelfRefGraphTest {
		@Test
		public void testSelfReferencingInserts(SessionFactoryScope scope) {
			runSelfReferencingTest(scope, "GRAPH", 50);
		}
	}

	// ========== Test implementations ==========

	private static void runSimpleInsertTest(SessionFactoryScope scope, String queueType, int entityCount) {
		Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		long startTime = System.nanoTime();

		scope.inTransaction(session -> {
			for (int i = 0; i < entityCount; i++) {
				session.persist(new SimpleEntity("Entity-" + i, i));
			}
		});

		long endTime = System.nanoTime();
		long durationMs = (endTime - startTime) / 1_000_000;

		System.out.println("\n========== Simple Insert Test ==========");
		System.out.println("Queue Type: " + queueType);
		System.out.println("Entity Count: " + entityCount);
		System.out.println("Duration: " + durationMs + " ms");
		System.out.println("Statements: " + stats.getPrepareStatementCount());
		System.out.println("========================================\n");
	}

	private static void runParentChildTest(SessionFactoryScope scope, String queueType, int parentCount) {
		Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		long startTime = System.nanoTime();

		scope.inTransaction(session -> {
			for (int i = 0; i < parentCount; i++) {
				Parent parent = new Parent("Parent-" + i);
				for (int j = 0; j < 10; j++) {
					parent.addChild(new Child("Child-" + i + "-" + j));
				}
				session.persist(parent);
			}
		});

		long endTime = System.nanoTime();
		long durationMs = (endTime - startTime) / 1_000_000;

		System.out.println("\n========== Parent-Child Test ==========");
		System.out.println("Queue Type: " + queueType);
		System.out.println("Parent Count: " + parentCount);
		System.out.println("Children per Parent: 10");
		System.out.println("Total Entities: " + (parentCount * 11));
		System.out.println("Duration: " + durationMs + " ms");
		System.out.println("Statements: " + stats.getPrepareStatementCount());
		System.out.println("========================================\n");
	}

	private static void runSelfReferencingTest(SessionFactoryScope scope, String queueType, int treeCount) {
		Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		long startTime = System.nanoTime();

		scope.inTransaction(session -> {
			for (int i = 0; i < treeCount; i++) {
				Node root = new Node("Root-" + i);

				// Create a tree: root -> 3 children -> each has 3 grandchildren
				for (int j = 0; j < 3; j++) {
					Node child = new Node("Child-" + i + "-" + j);
					root.addChild(child);

					for (int k = 0; k < 3; k++) {
						Node grandchild = new Node("Grandchild-" + i + "-" + j + "-" + k);
						child.addChild(grandchild);
					}
				}

				session.persist(root);
			}
		});

		long endTime = System.nanoTime();
		long durationMs = (endTime - startTime) / 1_000_000;

		int totalNodes = treeCount * (1 + 3 + 9); // root + children + grandchildren

		System.out.println("\n========== Self-Referencing Test ==========");
		System.out.println("Queue Type: " + queueType);
		System.out.println("Tree Count: " + treeCount);
		System.out.println("Total Nodes: " + totalNodes);
		System.out.println("Duration: " + durationMs + " ms");
		System.out.println("Statements: " + stats.getPrepareStatementCount());
		System.out.println("===========================================\n");
	}
}
