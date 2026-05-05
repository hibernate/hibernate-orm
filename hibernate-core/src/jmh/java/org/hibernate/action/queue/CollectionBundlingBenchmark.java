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
 * JMH Benchmarks for Collection Operation Bundling.
 *
 * Compares performance between:
 * - Legacy ActionQueue
 * - Graph-based ActionQueue without bundling (default)
 * - Graph-based ActionQueue with bundling enabled
 *
 * Run with:
 * ./gradlew :hibernate-core:jmh -Pjmh.include=".*CollectionBundlingBenchmark.*"
 *
 * Run specific benchmark:
 * ./gradlew :hibernate-core:jmh -Pjmh.include=".*CollectionBundlingBenchmark.collectionInsert_Small.*"
 *
 * @author Steve Ebersole
 */
@State(Scope.Benchmark)
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 1)
public class CollectionBundlingBenchmark {

	// ========== Entity Model ==========

	@Entity(name = "Document")
	@Table(name = "document")
	public static class Document {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;

		@ElementCollection
		@CollectionTable(name = "document_tags")
		private List<String> tags = new ArrayList<>();

		public Document() {}
		public Document(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Article")
	@Table(name = "article")
	public static class Article {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String title;

		@ElementCollection
		@CollectionTable(name = "article_keywords")
		private List<String> keywords = new ArrayList<>();

		@ElementCollection
		@CollectionTable(name = "article_categories")
		private List<String> categories = new ArrayList<>();

		public Article() {}
		public Article(String title) {
			this.title = title;
		}
	}

	// ========== State Classes ==========

	@State(Scope.Benchmark)
	public static class LegacyQueueState {
		SessionFactory sessionFactory;

		@Setup(Level.Trial)
		public void setup() {
			sessionFactory = createSessionFactory("legacy", false);
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			if (sessionFactory != null) {
				sessionFactory.close();
			}
		}
	}

	@State(Scope.Benchmark)
	public static class GraphQueueNoBundlingState {
		SessionFactory sessionFactory;

		@Setup(Level.Trial)
		public void setup() {
			sessionFactory = createSessionFactory("graph", false);
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			if (sessionFactory != null) {
				sessionFactory.close();
			}
		}
	}

	@State(Scope.Benchmark)
	public static class GraphQueueWithBundlingState {
		SessionFactory sessionFactory;

		@Setup(Level.Trial)
		public void setup() {
			sessionFactory = createSessionFactory("graph", true);
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			if (sessionFactory != null) {
				sessionFactory.close();
			}
		}
	}

	// ========== Helper Methods ==========

	private static SessionFactory createSessionFactory(String queueImpl, boolean bundling) {
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect")
				.applySetting(AvailableSettings.URL, "jdbc:h2:mem:collectionbench_" + queueImpl + "_" + bundling + ";DB_CLOSE_DELAY=-1")
				.applySetting(AvailableSettings.USER, "sa")
				.applySetting(AvailableSettings.PASS, "")
				.applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
				.applySetting(AvailableSettings.SHOW_SQL, "false")
				.applySetting(AvailableSettings.FORMAT_SQL, "false")
				.applySetting(AvailableSettings.USE_SQL_COMMENTS, "false")
				.applySetting(AvailableSettings.STATEMENT_BATCH_SIZE, "50")
				.applySetting(AvailableSettings.FLUSH_QUEUE_TYPE, queueImpl);

		if (bundling) {
			builder.applySetting("hibernate.bundle_collection_operations", "true");
		}

		ServiceRegistry registry = builder.build();

		return new MetadataSources(registry)
				.addAnnotatedClass(Document.class)
				.addAnnotatedClass(Article.class)
				.buildMetadata()
				.buildSessionFactory();
	}

	// ========================================================================
	// Small Collection Inserts (10 items)
	// ========================================================================

	@Benchmark
	public void collectionInsert_Small_Legacy(LegacyQueueState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 10, 10, blackhole);
	}

	@Benchmark
	public void collectionInsert_Small_GraphNoBundling(GraphQueueNoBundlingState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 10, 10, blackhole);
	}

	@Benchmark
	public void collectionInsert_Small_GraphBundling(GraphQueueWithBundlingState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 10, 10, blackhole);
	}

	// ========================================================================
	// Medium Collection Inserts (50 items)
	// ========================================================================

	@Benchmark
	public void collectionInsert_Medium_Legacy(LegacyQueueState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 10, 50, blackhole);
	}

	@Benchmark
	public void collectionInsert_Medium_GraphNoBundling(GraphQueueNoBundlingState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 10, 50, blackhole);
	}

	@Benchmark
	public void collectionInsert_Medium_GraphBundling(GraphQueueWithBundlingState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 10, 50, blackhole);
	}

	// ========================================================================
	// Large Collection Inserts (200 items)
	// ========================================================================

	@Benchmark
	public void collectionInsert_Large_Legacy(LegacyQueueState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 5, 200, blackhole);
	}

	@Benchmark
	public void collectionInsert_Large_GraphNoBundling(GraphQueueNoBundlingState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 5, 200, blackhole);
	}

	@Benchmark
	public void collectionInsert_Large_GraphBundling(GraphQueueWithBundlingState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 5, 200, blackhole);
	}

	private void insertDocuments(SessionFactory sf, int docCount, int tagsPerDoc, Blackhole blackhole) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < docCount; i++) {
				Document doc = new Document("Doc-" + i);
				for (int j = 0; j < tagsPerDoc; j++) {
					doc.tags.add("tag-" + j);
				}
				session.persist(doc);
			}
			session.getTransaction().commit();
			blackhole.consume(docCount);
		}
	}

	// ========================================================================
	// Collection Recreate (simulates update by recreate)
	// ========================================================================

	@Benchmark
	public void collectionRecreate_Legacy(LegacyQueueState state, Blackhole blackhole) {
		recreateCollections(state.sessionFactory, blackhole);
	}

	@Benchmark
	public void collectionRecreate_GraphNoBundling(GraphQueueNoBundlingState state, Blackhole blackhole) {
		recreateCollections(state.sessionFactory, blackhole);
	}

	@Benchmark
	public void collectionRecreate_GraphBundling(GraphQueueWithBundlingState state, Blackhole blackhole) {
		recreateCollections(state.sessionFactory, blackhole);
	}

	private void recreateCollections(SessionFactory sf, Blackhole blackhole) {
		// Insert documents with collections
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < 10; i++) {
				Document doc = new Document("RecreateDoc-" + i);
				for (int j = 0; j < 50; j++) {
					doc.tags.add("tag-" + j);
				}
				session.persist(doc);
			}
			session.getTransaction().commit();
			blackhole.consume(10);
		}
	}

	// ========================================================================
	// Multiple Collections per Entity
	// ========================================================================

	@Benchmark
	public void multipleCollections_Legacy(LegacyQueueState state, Blackhole blackhole) {
		insertArticles(state.sessionFactory, blackhole);
	}

	@Benchmark
	public void multipleCollections_GraphNoBundling(GraphQueueNoBundlingState state, Blackhole blackhole) {
		insertArticles(state.sessionFactory, blackhole);
	}

	@Benchmark
	public void multipleCollections_GraphBundling(GraphQueueWithBundlingState state, Blackhole blackhole) {
		insertArticles(state.sessionFactory, blackhole);
	}

	private void insertArticles(SessionFactory sf, Blackhole blackhole) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < 10; i++) {
				Article article = new Article("Article-" + i);
				for (int j = 0; j < 30; j++) {
					article.keywords.add("keyword-" + j);
				}
				for (int j = 0; j < 20; j++) {
					article.categories.add("category-" + j);
				}
				session.persist(article);
			}
			session.getTransaction().commit();
			blackhole.consume(10);
		}
	}

	// ========================================================================
	// Batch Processing with Collections
	// ========================================================================

	@Benchmark
	public void batchProcessing_Legacy(LegacyQueueState state, Blackhole blackhole) {
		batchInsert(state.sessionFactory, blackhole);
	}

	@Benchmark
	public void batchProcessing_GraphNoBundling(GraphQueueNoBundlingState state, Blackhole blackhole) {
		batchInsert(state.sessionFactory, blackhole);
	}

	@Benchmark
	public void batchProcessing_GraphBundling(GraphQueueWithBundlingState state, Blackhole blackhole) {
		batchInsert(state.sessionFactory, blackhole);
	}

	private void batchInsert(SessionFactory sf, Blackhole blackhole) {
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < 50; i++) {
				Document doc = new Document("BatchDoc-" + i);
				for (int j = 0; j < 100; j++) {
					doc.tags.add("tag-" + j);
				}
				session.persist(doc);
				if (i % 10 == 0) {
					session.flush();
					session.clear();
				}
			}
			session.getTransaction().commit();
			blackhole.consume(50);
		}
	}

	// ========================================================================
	// Mixed Operations (Insert + Update + Delete)
	// ========================================================================

	@Benchmark
	public void mixedOperations_Legacy(LegacyQueueState state, Blackhole blackhole) {
		mixedCollectionOperations(state.sessionFactory, blackhole);
	}

	@Benchmark
	public void mixedOperations_GraphNoBundling(GraphQueueNoBundlingState state, Blackhole blackhole) {
		mixedCollectionOperations(state.sessionFactory, blackhole);
	}

	@Benchmark
	public void mixedOperations_GraphBundling(GraphQueueWithBundlingState state, Blackhole blackhole) {
		mixedCollectionOperations(state.sessionFactory, blackhole);
	}

	private void mixedCollectionOperations(SessionFactory sf, Blackhole blackhole) {
		int operationCount = 0;

		// Insert phase
		List<Long> docIds = new ArrayList<>();
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < 15; i++) {
				Document doc = new Document("MixedDoc-" + i);
				for (int j = 0; j < 30; j++) {
					doc.tags.add("tag-" + j);
				}
				session.persist(doc);
				session.flush();
				docIds.add(doc.id);
				operationCount++;
			}
			session.getTransaction().commit();
		}

		// Update phase
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (int i = 0; i < docIds.size() / 2; i++) {
				Document doc = session.find(Document.class, docIds.get(i));
				doc.tags.remove(0);
				doc.tags.add("updated-tag");
				operationCount++;
			}
			session.getTransaction().commit();
		}

		// Delete phase
		try (Session session = sf.openSession()) {
			session.beginTransaction();
			for (Long id : docIds) {
				Document doc = session.find(Document.class, id);
				session.remove(doc);
				operationCount++;
			}
			session.getTransaction().commit();
		}

		blackhole.consume(operationCount);
	}

	// ========================================================================
	// Stress Test: Very Large Collections
	// ========================================================================

	@Benchmark
	public void stressTest_VeryLarge_Legacy(LegacyQueueState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 2, 500, blackhole);
	}

	@Benchmark
	public void stressTest_VeryLarge_GraphNoBundling(GraphQueueNoBundlingState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 2, 500, blackhole);
	}

	@Benchmark
	public void stressTest_VeryLarge_GraphBundling(GraphQueueWithBundlingState state, Blackhole blackhole) {
		insertDocuments(state.sessionFactory, 2, 500, blackhole);
	}
}
