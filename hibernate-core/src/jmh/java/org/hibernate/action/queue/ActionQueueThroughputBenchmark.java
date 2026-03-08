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
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Throughput JMH Benchmarks for ActionQueue implementations.
 *
 * Measures operations per second (ops/s) to compare throughput between:
 * - Legacy ActionQueue (org.hibernate.action.queue.ActionQueueLegacy)
 * - Graph-based ActionQueue (org.hibernate.action.queue.GraphBasedActionQueue)
 *
 * Run with:
 * ./gradlew :hibernate-core:jmh -Pjmh.include=".*ActionQueueThroughputBenchmark.*"
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(value = 1)
public class ActionQueueThroughputBenchmark {

	// ========== Entity Model ==========

	@Entity(name = "ThroughputEntity")
	@Table(name = "throughput_entity")
	public static class ThroughputEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String name;

		@Column(name = "entity_value")
		private int value;

		public ThroughputEntity() {}
		public ThroughputEntity(String name, int value) {
			this.name = name;
			this.value = value;
		}
	}

	@Entity(name = "ThroughputParent")
	@Table(name = "throughput_parent")
	public static class ThroughputParent {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<ThroughputChild> children = new ArrayList<>();

		public ThroughputParent() {}
		public ThroughputParent(String name) {
			this.name = name;
		}

		public void addChild(ThroughputChild child) {
			children.add(child);
			child.parent = this;
		}

		public void removeChild(ThroughputChild child) {
			children.remove(child);
			child.parent = null;
		}
	}

	@Entity(name = "ThroughputChild")
	@Table(name = "throughput_child")
	public static class ThroughputChild {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String name;
		private int data;

		@ManyToOne
		@JoinColumn(name = "parent_id")
		private ThroughputParent parent;

		public ThroughputChild() {}
		public ThroughputChild(String name, int data) {
			this.name = name;
			this.data = data;
		}
	}

	@Entity(name = "OrderedItem")
	@Table(name = "ordered_item")
	public static class OrderedItem {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String description;

		@ManyToOne
		@JoinColumn(name = "order_id")
		private OrderHeader order;

		public OrderedItem() {}
		public OrderedItem(String description) {
			this.description = description;
		}
	}

	@Entity(name = "OrderHeader")
	@Table(name = "order_header")
	public static class OrderHeader {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String orderNumber;
		private String customerName;

		@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
		@OrderColumn(name = "line_number")
		private List<OrderedItem> items = new ArrayList<>();

		public OrderHeader() {}
		public OrderHeader(String orderNumber, String customerName) {
			this.orderNumber = orderNumber;
			this.customerName = customerName;
		}

		public void addItem(OrderedItem item) {
			items.add(item);
			item.order = this;
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
				.applySetting(AvailableSettings.URL, "jdbc:h2:mem:throughput_" + queueImpl + ";DB_CLOSE_DELAY=-1")
				.applySetting(AvailableSettings.USER, "sa")
				.applySetting(AvailableSettings.PASS, "")
				.applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
				.applySetting(AvailableSettings.SHOW_SQL, "false")
				.applySetting(AvailableSettings.FORMAT_SQL, "false")
				.applySetting(AvailableSettings.USE_SQL_COMMENTS, "false")
				.applySetting(AvailableSettings.STATEMENT_BATCH_SIZE, "50")
				.applySetting("hibernate.flush.queue.impl", queueImpl)
				.build();

		return new MetadataSources(registry)
				.addAnnotatedClass(ThroughputEntity.class)
				.addAnnotatedClass(ThroughputParent.class)
				.addAnnotatedClass(ThroughputChild.class)
				.addAnnotatedClass(OrderHeader.class)
				.addAnnotatedClass(OrderedItem.class)
				.buildMetadata()
				.buildSessionFactory();
	}

	// ========== Throughput Benchmarks: Single Entity Insert ==========

	@Benchmark
	public void singleEntityInsert_Legacy(LegacyQueueState state, Blackhole bh) {
		try (Session session = state.sessionFactory.openSession()) {
			session.beginTransaction();
			ThroughputEntity entity = new ThroughputEntity("Entity", 42);
			session.persist(entity);
			session.getTransaction().commit();
			bh.consume(entity.id);
		}
	}

	@Benchmark
	public void singleEntityInsert_Graph(GraphQueueState state, Blackhole bh) {
		try (Session session = state.sessionFactory.openSession()) {
			session.beginTransaction();
			ThroughputEntity entity = new ThroughputEntity("Entity", 42);
			session.persist(entity);
			session.getTransaction().commit();
			bh.consume(entity.id);
		}
	}

	// ========== Throughput Benchmarks: Batch Insert ==========

	@Benchmark
	public void batchInsert_50_Legacy(LegacyQueueState state, Blackhole bh) {
		batchInsert(state.sessionFactory, 50, bh);
	}

	@Benchmark
	public void batchInsert_50_Graph(GraphQueueState state, Blackhole bh) {
		batchInsert(state.sessionFactory, 50, bh);
	}

	private void batchInsert(SessionFactory sf, int count, Blackhole bh) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < count; i++) {
				session.persist(new ThroughputEntity("Entity-" + i, i));
			}
			session.getTransaction().commit();
			bh.consume(session);
		}
	}

	// ========== Throughput Benchmarks: Parent-Child Insert ==========

	@Benchmark
	public void parentChildInsert_Legacy(LegacyQueueState state, Blackhole bh) {
		parentChildInsert(state.sessionFactory, bh);
	}

	@Benchmark
	public void parentChildInsert_Graph(GraphQueueState state, Blackhole bh) {
		parentChildInsert(state.sessionFactory, bh);
	}

	private void parentChildInsert(SessionFactory sf, Blackhole bh) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			ThroughputParent parent = new ThroughputParent("Parent");
			for (int i = 0; i < 10; i++) {
				parent.addChild(new ThroughputChild("Child-" + i, i));
			}
			session.persist(parent);
			session.getTransaction().commit();
			bh.consume(parent.id);
		}
	}

	// ========== Throughput Benchmarks: Update Operations ==========

	@Benchmark
	public void entityUpdate_Legacy(LegacyQueueState state, Blackhole bh) {
		entityUpdate(state.sessionFactory, bh);
	}

	@Benchmark
	public void entityUpdate_Graph(GraphQueueState state, Blackhole bh) {
		entityUpdate(state.sessionFactory, bh);
	}

	private void entityUpdate(SessionFactory sf, Blackhole bh) {
		// Insert
		Long id;
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			ThroughputEntity entity = new ThroughputEntity("Original", 1);
			session.persist(entity);
			session.getTransaction().commit();
			id = entity.id;
		}

		// Update
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			ThroughputEntity entity = session.find(ThroughputEntity.class, id);
			entity.name = "Updated";
			entity.value = 999;
			session.getTransaction().commit();
			bh.consume(entity);
		}

		// Cleanup
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			ThroughputEntity entity = session.find(ThroughputEntity.class, id);
			session.remove(entity);
			session.getTransaction().commit();
		}
	}

	// ========== Throughput Benchmarks: Collection Operations ==========

	@Benchmark
	public void collectionAdd_Legacy(LegacyQueueState state, Blackhole bh) {
		collectionAdd(state.sessionFactory, bh);
	}

	@Benchmark
	public void collectionAdd_Graph(GraphQueueState state, Blackhole bh) {
		collectionAdd(state.sessionFactory, bh);
	}

	private void collectionAdd(SessionFactory sf, Blackhole bh) {
		// Insert parent with initial children
		Long parentId;
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			ThroughputParent parent = new ThroughputParent("Parent");
			parent.addChild(new ThroughputChild("Child-1", 1));
			session.persist(parent);
			session.getTransaction().commit();
			parentId = parent.id;
		}

		// Add more children
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			ThroughputParent parent = session.find(ThroughputParent.class, parentId);
			for (int i = 2; i <= 5; i++) {
				parent.addChild(new ThroughputChild("Child-" + i, i));
			}
			session.getTransaction().commit();
			bh.consume(parent.children.size());
		}

		// Cleanup
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			ThroughputParent parent = session.find(ThroughputParent.class, parentId);
			session.remove(parent);
			session.getTransaction().commit();
		}
	}

	// ========== Throughput Benchmarks: OrderColumn Operations ==========

	@Benchmark
	public void orderColumnInsert_Legacy(LegacyQueueState state, Blackhole bh) {
		orderColumnInsert(state.sessionFactory, bh);
	}

	@Benchmark
	public void orderColumnInsert_Graph(GraphQueueState state, Blackhole bh) {
		orderColumnInsert(state.sessionFactory, bh);
	}

	private void orderColumnInsert(SessionFactory sf, Blackhole bh) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			OrderHeader order = new OrderHeader("ORD-001", "Customer");
			for (int i = 0; i < 20; i++) {
				order.addItem(new OrderedItem("Item-" + i));
			}
			session.persist(order);
			session.getTransaction().commit();
			bh.consume(order.id);
		}

		// Cleanup
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			session.createMutationQuery("delete from OrderedItem").executeUpdate();
			session.createMutationQuery("delete from OrderHeader").executeUpdate();
			session.getTransaction().commit();
		}
	}

	// ========== Throughput Benchmarks: Mixed Workload ==========

	@Benchmark
	public void mixedWorkload_Legacy(LegacyQueueState state, Blackhole bh) {
		mixedWorkload(state.sessionFactory, bh);
	}

	@Benchmark
	public void mixedWorkload_Graph(GraphQueueState state, Blackhole bh) {
		mixedWorkload(state.sessionFactory, bh);
	}

	private void mixedWorkload(SessionFactory sf, Blackhole bh) {
		List<Long> ids = new ArrayList<>();

		// Insert 10 entities
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < 10; i++) {
				ThroughputEntity entity = new ThroughputEntity("Entity-" + i, i);
				session.persist(entity);
				session.flush();
				ids.add(entity.id);
			}
			session.getTransaction().commit();
		}

		// Update half of them
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < 5; i++) {
				ThroughputEntity entity = session.find(ThroughputEntity.class, ids.get(i));
				entity.value = entity.value * 2;
			}
			session.getTransaction().commit();
		}

		// Delete all
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (Long id : ids) {
				ThroughputEntity entity = session.find(ThroughputEntity.class, id);
				session.remove(entity);
			}
			session.getTransaction().commit();
			bh.consume(ids.size());
		}
	}

	// ========== Throughput Benchmarks: Complex Cascade ==========

	@Benchmark
	public void complexCascade_Legacy(LegacyQueueState state, Blackhole bh) {
		complexCascade(state.sessionFactory, bh);
	}

	@Benchmark
	public void complexCascade_Graph(GraphQueueState state, Blackhole bh) {
		complexCascade(state.sessionFactory, bh);
	}

	private void complexCascade(SessionFactory sf, Blackhole bh) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			// Create 5 parents, each with 8 children
			for (int i = 0; i < 5; i++) {
				ThroughputParent parent = new ThroughputParent("Parent-" + i);
				for (int j = 0; j < 8; j++) {
					parent.addChild(new ThroughputChild("Child-" + i + "-" + j, j));
				}
				session.persist(parent);
			}
			session.getTransaction().commit();
			bh.consume(session);
		}

		// Cleanup
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			session.createMutationQuery("delete from ThroughputChild").executeUpdate();
			session.createMutationQuery("delete from ThroughputParent").executeUpdate();
			session.getTransaction().commit();
		}
	}
}
