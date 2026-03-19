/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.bundling;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark tests comparing performance of:
 * 1. Legacy ActionQueue
 * 2. Graph-based ActionQueue without bundling
 * 3. Graph-based ActionQueue with bundling
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		CollectionOperationBenchmarkTest.Document.class,
		CollectionOperationBenchmarkTest.LargeDocument.class
})
public class CollectionOperationBenchmarkTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.createMutationQuery("delete from Document").executeUpdate();
			session.createMutationQuery("delete from LargeDocument").executeUpdate();
		});
	}

	// ========================================================================
	// Small collection benchmarks (10 items)
	// ========================================================================

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "legacy")
	})
	public void benchmarkSmallCollectionInsert_Legacy(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 10, 100, "Small Insert - Legacy");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph")
	})
	public void benchmarkSmallCollectionInsert_GraphNoBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 10, 100, "Small Insert - Graph (No Bundling)");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void benchmarkSmallCollectionInsert_GraphWithBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 10, 100, "Small Insert - Graph (Bundling)");
		result.print();
	}

	// ========================================================================
	// Medium collection benchmarks (50 items)
	// ========================================================================

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "legacy")
	})
	public void benchmarkMediumCollectionInsert_Legacy(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 50, 50, "Medium Insert - Legacy");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph")
	})
	public void benchmarkMediumCollectionInsert_GraphNoBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 50, 50, "Medium Insert - Graph (No Bundling)");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void benchmarkMediumCollectionInsert_GraphWithBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 50, 50, "Medium Insert - Graph (Bundling)");
		result.print();
	}

	// ========================================================================
	// Large collection benchmarks (200 items)
	// ========================================================================

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "legacy")
	})
	public void benchmarkLargeCollectionInsert_Legacy(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 200, 20, "Large Insert - Legacy");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph")
	})
	public void benchmarkLargeCollectionInsert_GraphNoBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 200, 20, "Large Insert - Graph (No Bundling)");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void benchmarkLargeCollectionInsert_GraphWithBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 200, 20, "Large Insert - Graph (Bundling)");
		result.print();
	}

	// ========================================================================
	// Update benchmarks (50 items)
	// ========================================================================

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "legacy")
	})
	public void benchmarkCollectionUpdate_Legacy(SessionFactoryScope scope) {
		BenchmarkResult result = runUpdateBenchmark(scope, 50, 50, "Update - Legacy");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph")
	})
	public void benchmarkCollectionUpdate_GraphNoBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runUpdateBenchmark(scope, 50, 50, "Update - Graph (No Bundling)");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void benchmarkCollectionUpdate_GraphWithBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runUpdateBenchmark(scope, 50, 50, "Update - Graph (Bundling)");
		result.print();
	}

	// ========================================================================
	// Mixed operations benchmarks
	// ========================================================================

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "legacy")
	})
	public void benchmarkMixedOperations_Legacy(SessionFactoryScope scope) {
		BenchmarkResult result = runMixedOperationsBenchmark(scope, 30, "Mixed Ops - Legacy");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph")
	})
	public void benchmarkMixedOperations_GraphNoBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runMixedOperationsBenchmark(scope, 30, "Mixed Ops - Graph (No Bundling)");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true")
	})
	public void benchmarkMixedOperations_GraphWithBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runMixedOperationsBenchmark(scope, 30, "Mixed Ops - Graph (Bundling)");
		result.print();
	}

	// ========================================================================
	// Batch processing benchmarks (batching + bundling)
	// ========================================================================

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "legacy"),
			@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "50")
	})
	public void benchmarkBatchProcessing_Legacy(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 100, 20, "Batch Insert - Legacy");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "50")
	})
	public void benchmarkBatchProcessing_GraphNoBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 100, 20, "Batch Insert - Graph (No Bundling)");
		result.print();
	}

	@Test
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.FLUSH_QUEUE_IMPL, value = "graph"),
			@Setting(name = "hibernate.bundle_collection_operations", value = "true"),
			@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "50")
	})
	public void benchmarkBatchProcessing_GraphWithBundling(SessionFactoryScope scope) {
		BenchmarkResult result = runInsertBenchmark(scope, 100, 20, "Batch Insert - Graph (Bundling)");
		result.print();
	}

	// ========================================================================
	// Benchmark execution methods
	// ========================================================================

	private BenchmarkResult runInsertBenchmark(
			SessionFactoryScope scope,
			int collectionSize,
			int iterations,
			String description) {
		// Warmup
		for (int i = 0; i < 5; i++) {
			final int warmupId = i;
			scope.inTransaction(session -> {
				Document doc = new Document();
				doc.id = (long) warmupId;
				doc.tags = new ArrayList<>();
				for (int j = 0; j < collectionSize; j++) {
					doc.tags.add("tag_" + j);
				}
				session.persist(doc);
			});
		}
		cleanup(scope);

		// Actual benchmark
		long startTime = System.nanoTime();
		long startMemory = getUsedMemory();

		for (int i = 0; i < iterations; i++) {
			final int docId = i;
			scope.inTransaction(session -> {
				Document doc = new Document();
				doc.id = 1000L + docId;
				doc.tags = new ArrayList<>();
				for (int j = 0; j < collectionSize; j++) {
					doc.tags.add("tag_" + j);
				}
				session.persist(doc);
			});
		}

		long endTime = System.nanoTime();
		long endMemory = getUsedMemory();

		long durationNanos = endTime - startTime;
		long memoryUsed = endMemory - startMemory;

		return new BenchmarkResult(
				description,
				iterations,
				iterations * collectionSize,
				durationNanos,
				memoryUsed
		);
	}

	private BenchmarkResult runUpdateBenchmark(
			SessionFactoryScope scope,
			int collectionSize,
			int iterations,
			String description) {
		// Setup - create initial documents
		for (int i = 0; i < iterations; i++) {
			final int docId = i;
			scope.inTransaction(session -> {
				Document doc = new Document();
				doc.id = (long) docId;
				doc.tags = new ArrayList<>();
				for (int j = 0; j < collectionSize; j++) {
					doc.tags.add("initial_" + j);
				}
				session.persist(doc);
			});
		}

		// Warmup
		for (int i = 0; i < 5; i++) {
			final int docId = i;
			scope.inTransaction(session -> {
				Document doc = session.find(Document.class, (long) docId);
				doc.tags.remove(0); // delete one
				doc.tags.add("new_tag"); // add one
			});
		}

		// Actual benchmark
		long startTime = System.nanoTime();
		long startMemory = getUsedMemory();

		for (int i = 0; i < iterations; i++) {
			final int docId = i;
			scope.inTransaction(session -> {
				Document doc = session.find(Document.class, (long) docId);
				// Remove 25% of items
				int toRemove = collectionSize / 4;
				for (int j = 0; j < toRemove; j++) {
					if (!doc.tags.isEmpty()) {
						doc.tags.remove(0);
					}
				}
				// Add new items
				for (int j = 0; j < toRemove; j++) {
					doc.tags.add("updated_" + j);
				}
			});
		}

		long endTime = System.nanoTime();
		long endMemory = getUsedMemory();

		long durationNanos = endTime - startTime;
		long memoryUsed = endMemory - startMemory;

		return new BenchmarkResult(
				description,
				iterations,
				iterations * (collectionSize / 2), // Approximate ops: deletes + inserts
				durationNanos,
				memoryUsed
		);
	}

	private BenchmarkResult runMixedOperationsBenchmark(
			SessionFactoryScope scope,
			int iterations,
			String description) {
		// Warmup
		for (int i = 0; i < 3; i++) {
			performMixedOperations(scope, i);
		}
		cleanup(scope);

		// Actual benchmark
		long startTime = System.nanoTime();
		long startMemory = getUsedMemory();

		int totalOperations = 0;
		for (int i = 0; i < iterations; i++) {
			totalOperations += performMixedOperations(scope, i);
		}

		long endTime = System.nanoTime();
		long endMemory = getUsedMemory();

		long durationNanos = endTime - startTime;
		long memoryUsed = endMemory - startMemory;

		return new BenchmarkResult(
				description,
				iterations,
				totalOperations,
				durationNanos,
				memoryUsed
		);
	}

	private int performMixedOperations(SessionFactoryScope scope, int iteration) {
		return scope.fromTransaction(session -> {
			int opCount = 0;

			// Insert new documents with collections
			for (int i = 0; i < 5; i++) {
				Document doc = new Document();
				doc.id = iteration * 1000L + i;
				doc.tags = new ArrayList<>();
				for (int j = 0; j < 20; j++) {
					doc.tags.add("tag_" + j);
				}
				session.persist(doc);
				opCount += 20; // 20 collection inserts
			}

			// Update existing (from previous iterations)
			if (iteration > 0) {
				Long previousId = (iteration - 1) * 1000L;
				Document doc = session.find(Document.class, previousId);
				if (doc != null) {
					doc.tags.remove(0);
					doc.tags.add("new_tag");
					opCount += 2; // 1 delete + 1 insert
				}
			}

			return opCount;
		});
	}

	private long getUsedMemory() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	// ========================================================================
	// Test entities
	// ========================================================================

	@Entity(name = "Document")
	public static class Document {
		@Id
		Long id;

		@ElementCollection
		List<String> tags;
	}

	@Entity(name = "LargeDocument")
	public static class LargeDocument {
		@Id
		Long id;

		@ElementCollection
		List<String> metadata;

		@ElementCollection
		List<String> keywords;
	}

	// ========================================================================
	// Benchmark result tracking
	// ========================================================================

	private static class BenchmarkResult {
		final String description;
		final int iterations;
		final int totalOperations;
		final long durationNanos;
		final long memoryUsedBytes;

		BenchmarkResult(
				String description,
				int iterations,
				int totalOperations,
				long durationNanos,
				long memoryUsedBytes) {
			this.description = description;
			this.iterations = iterations;
			this.totalOperations = totalOperations;
			this.durationNanos = durationNanos;
			this.memoryUsedBytes = memoryUsedBytes;
		}

		void print() {
			double avgTimeMs = TimeUnit.NANOSECONDS.toMillis(durationNanos) / (double) iterations;
			double totalTimeMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);
			double opsPerSecond = (totalOperations / (durationNanos / 1_000_000_000.0));
			double memoryMB = memoryUsedBytes / (1024.0 * 1024.0);

			System.out.println("=".repeat(80));
			System.out.println("BENCHMARK: " + description);
			System.out.println("=".repeat(80));
			System.out.printf("  Iterations:         %,d%n", iterations);
			System.out.printf("  Total Operations:   %,d%n", totalOperations);
			System.out.printf("  Total Time:         %.2f ms%n", totalTimeMs);
			System.out.printf("  Avg Time/Iter:      %.3f ms%n", avgTimeMs);
			System.out.printf("  Throughput:         %,.0f ops/sec%n", opsPerSecond);
			System.out.printf("  Memory Delta:       %.2f MB%n", memoryMB);
			System.out.println("=".repeat(80));
		}
	}
}
