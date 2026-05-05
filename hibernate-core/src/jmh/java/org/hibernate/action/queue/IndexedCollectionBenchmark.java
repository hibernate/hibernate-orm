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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmarks for Indexed Collection Operations.
 *
 * Tests performance of @OrderColumn collections with various operations:
 * - Shifts (position changes)
 * - Additions (insert at various positions)
 * - Removals (delete from various positions)
 * - Mixed operations
 *
 * Validates the performance benefits of:
 * - Change-set based approach (O(N+M) vs O(N²))
 * - Two-phase unique constraint break (no O(N²) graph edges)
 * - JDBC batching with same ordinals
 *
 * Run with:
 * ./gradlew :hibernate-core:jmh -Pjmh.include=".*IndexedCollectionBenchmark.*"
 *
 * Run specific benchmark:
 * ./gradlew :hibernate-core:jmh -Pjmh.include=".*IndexedCollectionBenchmark.shift_Small.*"
 *
 * @author Steve Ebersole
 */
@State(Scope.Benchmark)
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 1)
public class IndexedCollectionBenchmark {

	// ========== Entity Model ==========

	@Entity(name = "IndexedParent")
	@Table(name = "indexed_parent")
	public static class Parent {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		@OrderColumn(name = "listOrder")
		private List<Child> children = new ArrayList<>();

		public Parent() {}
		public Parent(String name) {
			this.name = name;
		}

		public void addChild(Child child) {
			children.add(child);
		}

		public List<Child> getChildren() {
			return children;
		}
	}

	@Entity(name = "IndexedChild")
	@Table(name = "indexed_child")
	public static class Child {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;
		@Column(name = "val")
		private int value;

		public Child() {}
		public Child(String name, int value) {
			this.name = name;
			this.value = value;
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
				.applySetting(AvailableSettings.URL, "jdbc:h2:mem:indexedbench_" + queueImpl + ";DB_CLOSE_DELAY=-1")
				.applySetting(AvailableSettings.USER, "sa")
				.applySetting(AvailableSettings.PASS, "")
				.applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
				.applySetting(AvailableSettings.SHOW_SQL, "false")
				.applySetting(AvailableSettings.FORMAT_SQL, "false")
				.applySetting(AvailableSettings.USE_SQL_COMMENTS, "false")
				.applySetting(AvailableSettings.STATEMENT_BATCH_SIZE, "50")
				.applySetting(AvailableSettings.FLUSH_QUEUE_TYPE, queueImpl)
				.build();

		return new MetadataSources(registry)
				.addAnnotatedClass(Parent.class)
				.addAnnotatedClass(Child.class)
				.buildMetadata()
				.buildSessionFactory();
	}

	private Long createParentWithChildren(SessionFactory sf, int childCount) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = new Parent("Parent");
			for (int i = 0; i < childCount; i++) {
				parent.addChild(new Child("Child-" + i, i));
			}
			session.persist(parent);
			session.getTransaction().commit();
			return parent.id;
		}
	}

	private void cleanup(SessionFactory sf, Long parentId) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = session.find(Parent.class, parentId);
			if (parent != null) {
				session.remove(parent);
			}
			session.getTransaction().commit();
		}
	}

	// ========================================================================
	// SHIFT OPERATIONS - Position changes without add/remove
	// ========================================================================

	// Small: 10 elements, swap first and last
	@Benchmark
	public void shift_Small_SwapFirstLast_Legacy(LegacyQueueState state, Blackhole bh) {
		shiftSwapFirstLast(state.sessionFactory, 10, bh);
	}

	@Benchmark
	public void shift_Small_SwapFirstLast_Graph(GraphQueueState state, Blackhole bh) {
		shiftSwapFirstLast(state.sessionFactory, 10, bh);
	}

	private void shiftSwapFirstLast(SessionFactory sf, int size, Blackhole bh) {
		Long parentId = createParentWithChildren(sf, size);

		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = session.find(Parent.class, parentId);
			List<Child> children = parent.getChildren();

			// Swap first and last elements
			Child first = children.get(0);
			Child last = children.get(size - 1);
			children.set(0, last);
			children.set(size - 1, first);

			session.getTransaction().commit();
			bh.consume(children.size());
		}

		cleanup(sf, parentId);
	}

	// Small: 10 elements, reverse entire list
	@Benchmark
	public void shift_Small_Reverse_Legacy(LegacyQueueState state, Blackhole bh) {
		shiftReverse(state.sessionFactory, 10, bh);
	}

	@Benchmark
	public void shift_Small_Reverse_Graph(GraphQueueState state, Blackhole bh) {
		shiftReverse(state.sessionFactory, 10, bh);
	}

	private void shiftReverse(SessionFactory sf, int size, Blackhole bh) {
		Long parentId = createParentWithChildren(sf, size);

		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = session.find(Parent.class, parentId);
			List<Child> children = parent.getChildren();

			// Reverse the list - every element changes position
			Collections.reverse(children);

			session.getTransaction().commit();
			bh.consume(children.size());
		}

		cleanup(sf, parentId);
	}

	// Medium: 50 elements, swap first and last
	@Benchmark
	public void shift_Medium_SwapFirstLast_Legacy(LegacyQueueState state, Blackhole bh) {
		shiftSwapFirstLast(state.sessionFactory, 50, bh);
	}

	@Benchmark
	public void shift_Medium_SwapFirstLast_Graph(GraphQueueState state, Blackhole bh) {
		shiftSwapFirstLast(state.sessionFactory, 50, bh);
	}

	// Medium: 50 elements, reverse entire list
	@Benchmark
	public void shift_Medium_Reverse_Legacy(LegacyQueueState state, Blackhole bh) {
		shiftReverse(state.sessionFactory, 50, bh);
	}

	@Benchmark
	public void shift_Medium_Reverse_Graph(GraphQueueState state, Blackhole bh) {
		shiftReverse(state.sessionFactory, 50, bh);
	}

	// Large: 200 elements, swap first and last
	@Benchmark
	public void shift_Large_SwapFirstLast_Legacy(LegacyQueueState state, Blackhole bh) {
		shiftSwapFirstLast(state.sessionFactory, 200, bh);
	}

	@Benchmark
	public void shift_Large_SwapFirstLast_Graph(GraphQueueState state, Blackhole bh) {
		shiftSwapFirstLast(state.sessionFactory, 200, bh);
	}

	// Large: 200 elements, reverse entire list
	@Benchmark
	public void shift_Large_Reverse_Legacy(LegacyQueueState state, Blackhole bh) {
		shiftReverse(state.sessionFactory, 200, bh);
	}

	@Benchmark
	public void shift_Large_Reverse_Graph(GraphQueueState state, Blackhole bh) {
		shiftReverse(state.sessionFactory, 200, bh);
	}

	// Large: 200 elements, rotate by 10
	@Benchmark
	public void shift_Large_Rotate_Legacy(LegacyQueueState state, Blackhole bh) {
		shiftRotate(state.sessionFactory, 200, 10, bh);
	}

	@Benchmark
	public void shift_Large_Rotate_Graph(GraphQueueState state, Blackhole bh) {
		shiftRotate(state.sessionFactory, 200, 10, bh);
	}

	private void shiftRotate(SessionFactory sf, int size, int distance, Blackhole bh) {
		Long parentId = createParentWithChildren(sf, size);

		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = session.find(Parent.class, parentId);
			List<Child> children = parent.getChildren();

			// Rotate list by distance positions
			Collections.rotate(children, distance);

			session.getTransaction().commit();
			bh.consume(children.size());
		}

		cleanup(sf, parentId);
	}

	// ========================================================================
	// REMOVAL OPERATIONS - Delete from various positions
	// ========================================================================

	// Small: 10 elements, remove first
	@Benchmark
	public void remove_Small_First_Legacy(LegacyQueueState state, Blackhole bh) {
		removeAtPosition(state.sessionFactory, 10, 0, bh);
	}

	@Benchmark
	public void remove_Small_First_Graph(GraphQueueState state, Blackhole bh) {
		removeAtPosition(state.sessionFactory, 10, 0, bh);
	}

	// Small: 10 elements, remove middle
	@Benchmark
	public void remove_Small_Middle_Legacy(LegacyQueueState state, Blackhole bh) {
		removeAtPosition(state.sessionFactory, 10, 5, bh);
	}

	@Benchmark
	public void remove_Small_Middle_Graph(GraphQueueState state, Blackhole bh) {
		removeAtPosition(state.sessionFactory, 10, 5, bh);
	}

	// Small: 10 elements, remove last
	@Benchmark
	public void remove_Small_Last_Legacy(LegacyQueueState state, Blackhole bh) {
		removeAtPosition(state.sessionFactory, 10, 9, bh);
	}

	@Benchmark
	public void remove_Small_Last_Graph(GraphQueueState state, Blackhole bh) {
		removeAtPosition(state.sessionFactory, 10, 9, bh);
	}

	private void removeAtPosition(SessionFactory sf, int size, int position, Blackhole bh) {
		Long parentId = createParentWithChildren(sf, size);

		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = session.find(Parent.class, parentId);
			List<Child> children = parent.getChildren();

			children.remove(position);

			session.getTransaction().commit();
			bh.consume(children.size());
		}

		cleanup(sf, parentId);
	}

	// Medium: 50 elements, remove first
	@Benchmark
	public void remove_Medium_First_Legacy(LegacyQueueState state, Blackhole bh) {
		removeAtPosition(state.sessionFactory, 50, 0, bh);
	}

	@Benchmark
	public void remove_Medium_First_Graph(GraphQueueState state, Blackhole bh) {
		removeAtPosition(state.sessionFactory, 50, 0, bh);
	}

	// Large: 200 elements, remove first
	@Benchmark
	public void remove_Large_First_Legacy(LegacyQueueState state, Blackhole bh) {
		removeAtPosition(state.sessionFactory, 200, 0, bh);
	}

	@Benchmark
	public void remove_Large_First_Graph(GraphQueueState state, Blackhole bh) {
		removeAtPosition(state.sessionFactory, 200, 0, bh);
	}

	// Large: 200 elements, remove multiple (10%)
	@Benchmark
	public void remove_Large_Multiple_Legacy(LegacyQueueState state, Blackhole bh) {
		removeMultiple(state.sessionFactory, 200, 20, bh);
	}

	@Benchmark
	public void remove_Large_Multiple_Graph(GraphQueueState state, Blackhole bh) {
		removeMultiple(state.sessionFactory, 200, 20, bh);
	}

	private void removeMultiple(SessionFactory sf, int size, int removeCount, Blackhole bh) {
		Long parentId = createParentWithChildren(sf, size);

		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = session.find(Parent.class, parentId);
			List<Child> children = parent.getChildren();

			// Remove every Nth element
			int step = size / removeCount;
			for (int i = 0; i < removeCount; i++) {
				children.remove(0);  // Always remove first after previous removal
			}

			session.getTransaction().commit();
			bh.consume(children.size());
		}

		cleanup(sf, parentId);
	}

	// ========================================================================
	// ADDITION OPERATIONS - Insert at various positions
	// ========================================================================

	// Small: 10 elements, add at beginning
	@Benchmark
	public void add_Small_First_Legacy(LegacyQueueState state, Blackhole bh) {
		addAtPosition(state.sessionFactory, 10, 0, bh);
	}

	@Benchmark
	public void add_Small_First_Graph(GraphQueueState state, Blackhole bh) {
		addAtPosition(state.sessionFactory, 10, 0, bh);
	}

	// Small: 10 elements, add at middle
	@Benchmark
	public void add_Small_Middle_Legacy(LegacyQueueState state, Blackhole bh) {
		addAtPosition(state.sessionFactory, 10, 5, bh);
	}

	@Benchmark
	public void add_Small_Middle_Graph(GraphQueueState state, Blackhole bh) {
		addAtPosition(state.sessionFactory, 10, 5, bh);
	}

	// Small: 10 elements, add at end
	@Benchmark
	public void add_Small_Last_Legacy(LegacyQueueState state, Blackhole bh) {
		addAtPosition(state.sessionFactory, 10, 10, bh);
	}

	@Benchmark
	public void add_Small_Last_Graph(GraphQueueState state, Blackhole bh) {
		addAtPosition(state.sessionFactory, 10, 10, bh);
	}

	private void addAtPosition(SessionFactory sf, int size, int position, Blackhole bh) {
		Long parentId = createParentWithChildren(sf, size);

		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = session.find(Parent.class, parentId);
			List<Child> children = parent.getChildren();

			children.add(position, new Child("New-" + position, 999));

			session.getTransaction().commit();
			bh.consume(children.size());
		}

		cleanup(sf, parentId);
	}

	// Medium: 50 elements, add at beginning
	@Benchmark
	public void add_Medium_First_Legacy(LegacyQueueState state, Blackhole bh) {
		addAtPosition(state.sessionFactory, 50, 0, bh);
	}

	@Benchmark
	public void add_Medium_First_Graph(GraphQueueState state, Blackhole bh) {
		addAtPosition(state.sessionFactory, 50, 0, bh);
	}

	// Large: 200 elements, add at beginning
	@Benchmark
	public void add_Large_First_Legacy(LegacyQueueState state, Blackhole bh) {
		addAtPosition(state.sessionFactory, 200, 0, bh);
	}

	@Benchmark
	public void add_Large_First_Graph(GraphQueueState state, Blackhole bh) {
		addAtPosition(state.sessionFactory, 200, 0, bh);
	}

	// Large: 200 elements, add multiple (10%)
	@Benchmark
	public void add_Large_Multiple_Legacy(LegacyQueueState state, Blackhole bh) {
		addMultiple(state.sessionFactory, 200, 20, bh);
	}

	@Benchmark
	public void add_Large_Multiple_Graph(GraphQueueState state, Blackhole bh) {
		addMultiple(state.sessionFactory, 200, 20, bh);
	}

	private void addMultiple(SessionFactory sf, int size, int addCount, Blackhole bh) {
		Long parentId = createParentWithChildren(sf, size);

		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = session.find(Parent.class, parentId);
			List<Child> children = parent.getChildren();

			// Add elements at regular intervals
			int step = size / addCount;
			for (int i = 0; i < addCount; i++) {
				int position = i * step;
				children.add(position, new Child("New-" + i, 999 + i));
			}

			session.getTransaction().commit();
			bh.consume(children.size());
		}

		cleanup(sf, parentId);
	}

	// ========================================================================
	// MIXED OPERATIONS - Combinations of shifts, adds, removes
	// ========================================================================

	// Small: 10 elements, remove first + add at middle
	@Benchmark
	public void mixed_Small_RemoveAdd_Legacy(LegacyQueueState state, Blackhole bh) {
		mixedRemoveAdd(state.sessionFactory, 10, bh);
	}

	@Benchmark
	public void mixed_Small_RemoveAdd_Graph(GraphQueueState state, Blackhole bh) {
		mixedRemoveAdd(state.sessionFactory, 10, bh);
	}

	private void mixedRemoveAdd(SessionFactory sf, int size, Blackhole bh) {
		Long parentId = createParentWithChildren(sf, size);

		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = session.find(Parent.class, parentId);
			List<Child> children = parent.getChildren();

			// Remove first element
			children.remove(0);
			// Add new element in middle
			children.add(children.size() / 2, new Child("New-Middle", 999));

			session.getTransaction().commit();
			bh.consume(children.size());
		}

		cleanup(sf, parentId);
	}

	// Medium: 50 elements, complex mixed operations
	@Benchmark
	public void mixed_Medium_Complex_Legacy(LegacyQueueState state, Blackhole bh) {
		mixedComplex(state.sessionFactory, 50, bh);
	}

	@Benchmark
	public void mixed_Medium_Complex_Graph(GraphQueueState state, Blackhole bh) {
		mixedComplex(state.sessionFactory, 50, bh);
	}

	private void mixedComplex(SessionFactory sf, int size, Blackhole bh) {
		Long parentId = createParentWithChildren(sf, size);

		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = session.find(Parent.class, parentId);
			List<Child> children = parent.getChildren();

			// Remove multiple elements
			children.remove(0);
			children.remove(size / 2 - 1);
			children.remove(children.size() - 1);

			// Add new elements
			children.add(0, new Child("New-0", 1000));
			children.add(children.size(), new Child("New-Last", 1001));

			// Swap some elements
			Child first = children.get(0);
			Child last = children.get(children.size() - 1);
			children.set(0, last);
			children.set(children.size() - 1, first);

			session.getTransaction().commit();
			bh.consume(children.size());
		}

		cleanup(sf, parentId);
	}

	// Large: 200 elements, realistic update scenario
	@Benchmark
	public void mixed_Large_Realistic_Legacy(LegacyQueueState state, Blackhole bh) {
		mixedRealistic(state.sessionFactory, 200, bh);
	}

	@Benchmark
	public void mixed_Large_Realistic_Graph(GraphQueueState state, Blackhole bh) {
		mixedRealistic(state.sessionFactory, 200, bh);
	}

	private void mixedRealistic(SessionFactory sf, int size, Blackhole bh) {
		Long parentId = createParentWithChildren(sf, size);

		try (Session session = sf.openSession()) {
			session.beginTransaction();
			Parent parent = session.find(Parent.class, parentId);
			List<Child> children = parent.getChildren();

			// Realistic scenario:
			// - Remove 5% of elements
			// - Add 3% new elements
			// - Shift 10% of elements

			int removeCount = size / 20;  // 5%
			int addCount = size / 33;      // ~3%
			int shiftCount = size / 10;    // 10%

			// Removals
			for (int i = 0; i < removeCount; i++) {
				children.remove(i * 4);  // Spread out removals
			}

			// Additions
			for (int i = 0; i < addCount; i++) {
				children.add(i * 10, new Child("New-" + i, 2000 + i));
			}

			// Shifts - swap pairs
			for (int i = 0; i < shiftCount; i += 2) {
				if (i + 1 < children.size()) {
					Child temp = children.get(i);
					children.set(i, children.get(i + 1));
					children.set(i + 1, temp);
				}
			}

			session.getTransaction().commit();
			bh.consume(children.size());
		}

		cleanup(sf, parentId);
	}

	// ========================================================================
	// STRESS TESTS - Extreme scenarios
	// ========================================================================

	// Very Large: 500 elements, reverse all
	@Benchmark
	public void stress_VeryLarge_Reverse_Legacy(LegacyQueueState state, Blackhole bh) {
		shiftReverse(state.sessionFactory, 500, bh);
	}

	@Benchmark
	public void stress_VeryLarge_Reverse_Graph(GraphQueueState state, Blackhole bh) {
		shiftReverse(state.sessionFactory, 500, bh);
	}

	// Very Large: 500 elements, remove half
	@Benchmark
	public void stress_VeryLarge_RemoveHalf_Legacy(LegacyQueueState state, Blackhole bh) {
		removeMultiple(state.sessionFactory, 500, 250, bh);
	}

	@Benchmark
	public void stress_VeryLarge_RemoveHalf_Graph(GraphQueueState state, Blackhole bh) {
		removeMultiple(state.sessionFactory, 500, 250, bh);
	}

	// Very Large: 500 elements, add 100
	@Benchmark
	public void stress_VeryLarge_AddMany_Legacy(LegacyQueueState state, Blackhole bh) {
		addMultiple(state.sessionFactory, 500, 100, bh);
	}

	@Benchmark
	public void stress_VeryLarge_AddMany_Graph(GraphQueueState state, Blackhole bh) {
		addMultiple(state.sessionFactory, 500, 100, bh);
	}
}
