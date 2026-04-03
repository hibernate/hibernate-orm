/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmarks for ActionQueue implementations.
 *
 * Compares performance between:
 * - Legacy ActionQueue (org.hibernate.action.queue.ActionQueueLegacy)
 * - Graph-based ActionQueue (org.hibernate.action.queue.GraphBasedActionQueue)
 *
 * Run with:
 * ./gradlew :hibernate-core:jmh -Pjmh.include=".*ActionQueueBenchmark.*"
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 1)
public class ActionQueueBenchmark {

	// ========== Entity Model ==========

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

	// ========== State Classes ==========

	@State(Scope.Benchmark)
	public static class LegacyQueueState {
		SessionFactory sessionFactory;

		@Setup(Level.Trial)
		public void setup() {
			sessionFactory = createSessionFactory("legacy");
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			if (sessionFactory != null) {
				sessionFactory.close();
			}
		}
	}

	@State(Scope.Benchmark)
	public static class GraphQueueState {
		SessionFactory sessionFactory;

		@Setup(Level.Trial)
		public void setup() {
			sessionFactory = createSessionFactory("graph");
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			if (sessionFactory != null) {
				sessionFactory.close();
			}
		}
	}

	// ========== Helper Methods ==========

	private static SessionFactory createSessionFactory(String queueImpl) {
		ServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect")
				.applySetting(AvailableSettings.URL, "jdbc:h2:mem:testdb_" + queueImpl + ";DB_CLOSE_DELAY=-1")
				.applySetting(AvailableSettings.USER, "sa")
				.applySetting(AvailableSettings.PASS, "")
				.applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
				.applySetting(AvailableSettings.SHOW_SQL, "false")
				.applySetting(AvailableSettings.FORMAT_SQL, "false")
				.applySetting(AvailableSettings.USE_SQL_COMMENTS, "false")
				.applySetting(AvailableSettings.STATEMENT_BATCH_SIZE, "50")
				.applySetting( "hibernate.flush.queue.type", queueImpl)
				.build();

		return new MetadataSources(registry)
				.addAnnotatedClass(SimpleEntity.class)
				.addAnnotatedClass(Parent.class)
				.addAnnotatedClass(Child.class)
				.addAnnotatedClass(Node.class)
				.buildMetadata()
				.buildSessionFactory();
	}

	// ========== Benchmarks: Simple Inserts ==========

	@Benchmark
	public void simpleInserts_100_Legacy(LegacyQueueState state) {
		simpleInserts(state.sessionFactory, 100);
	}

	@Benchmark
	public void simpleInserts_100_Graph(GraphQueueState state) {
		simpleInserts(state.sessionFactory, 100);
	}

	@Benchmark
	public void simpleInserts_1000_Legacy(LegacyQueueState state) {
		simpleInserts(state.sessionFactory, 1000);
	}

	@Benchmark
	public void simpleInserts_1000_Graph(GraphQueueState state) {
		simpleInserts(state.sessionFactory, 1000);
	}

	private void simpleInserts(SessionFactory sf, int count) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < count; i++) {
				session.persist(new SimpleEntity("Entity-" + i, i));
			}
			session.getTransaction().commit();
		}
	}

	// ========== Benchmarks: Parent-Child Inserts ==========

	@Benchmark
	public void parentChild_10_Legacy(LegacyQueueState state) {
		parentChildInserts(state.sessionFactory, 10);
	}

	@Benchmark
	public void parentChild_10_Graph(GraphQueueState state) {
		parentChildInserts(state.sessionFactory, 10);
	}

	@Benchmark
	public void parentChild_100_Legacy(LegacyQueueState state) {
		parentChildInserts(state.sessionFactory, 100);
	}

	@Benchmark
	public void parentChild_100_Graph(GraphQueueState state) {
		parentChildInserts(state.sessionFactory, 100);
	}

	private void parentChildInserts(SessionFactory sf, int parentCount) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < parentCount; i++) {
				Parent parent = new Parent("Parent-" + i);
				for (int j = 0; j < 10; j++) {
					parent.addChild(new Child("Child-" + i + "-" + j));
				}
				session.persist(parent);
			}
			session.getTransaction().commit();
		}
	}

	// ========== Benchmarks: Self-Referencing Trees ==========

	@Benchmark
	public void selfRefTree_10_Legacy(LegacyQueueState state) {
		selfReferencingTrees(state.sessionFactory, 10);
	}

	@Benchmark
	public void selfRefTree_10_Graph(GraphQueueState state) {
		selfReferencingTrees(state.sessionFactory, 10);
	}

	@Benchmark
	public void selfRefTree_50_Legacy(LegacyQueueState state) {
		selfReferencingTrees(state.sessionFactory, 50);
	}

	@Benchmark
	public void selfRefTree_50_Graph(GraphQueueState state) {
		selfReferencingTrees(state.sessionFactory, 50);
	}

	private void selfReferencingTrees(SessionFactory sf, int treeCount) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
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
			session.getTransaction().commit();
		}
	}

	// ========== Benchmarks: Mixed Operations ==========

	@Benchmark
	public void mixedOperations_Legacy(LegacyQueueState state) {
		mixedOperations(state.sessionFactory);
	}

	@Benchmark
	public void mixedOperations_Graph(GraphQueueState state) {
		mixedOperations(state.sessionFactory);
	}

	private void mixedOperations(SessionFactory sf) {
		// Insert phase
		List<Long> parentIds = new ArrayList<>();
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < 50; i++) {
				Parent parent = new Parent("Parent-" + i);
				for (int j = 0; j < 5; j++) {
					parent.addChild(new Child("Child-" + i + "-" + j));
				}
				session.persist(parent);
				session.flush();
				parentIds.add(parent.id);
			}
			session.getTransaction().commit();
		}

		// Update phase
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (Long id : parentIds) {
				Parent parent = session.find(Parent.class, id);
				parent.name = "Updated-" + parent.name;
			}
			session.getTransaction().commit();
		}

		// Delete phase
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (Long id : parentIds) {
				Parent parent = session.find(Parent.class, id);
				session.remove(parent);
			}
			session.getTransaction().commit();
		}
	}
}
