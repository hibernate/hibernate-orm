/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.hibernate.graph.GraphSemantic;
import org.hibernate.orm.test.entitygraph.EntityGraphBatchSizeTest_.Book_;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.BatchFetch;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Fetch;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.Hibernate.isInitialized;

@DomainModel(
		annotatedClasses = {
				EntityGraphBatchSizeTest.Book.class,
				EntityGraphBatchSizeTest.BatchedAuthor.class,
				EntityGraphBatchSizeTest.SingleAuthor.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
class EntityGraphBatchSizeTest {
	private static final String NAMED_GRAPH = "Book.with-batch-sizes";

	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				final var batchedAuthor = new BatchedAuthor( id );
				final var singleAuthor = new SingleAuthor( id );
				session.persist( batchedAuthor );
				session.persist( singleAuthor );
				session.persist( new Book( id, batchedAuthor, singleAuthor ) );
			}
		} );
		scope.getCollectingStatementInspector().clear();
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void programmaticGraphBatchSizeControlsAssociationBatching(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Book.class );
			graph.addAttributeNode( Book_.batchedAuthor )
					.addOption( FetchType.EAGER )
					.addOption( new BatchFetch( 3 ) );
			graph.addAttributeNode( Book_.singleAuthor )
					.addOption( FetchType.EAGER )
					.addOption( new BatchFetch( 1 ) );
			graph.addAttributeNode( Book_.batchedTags )
					.addOption( FetchType.EAGER )
					.addOption( new BatchFetch( 3 ) );
			graph.addAttributeNode( Book_.singleTags )
					.addOption( FetchType.EAGER )
					.addOption( new BatchFetch( 1 ) );

			final var books =
					session.createQuery( "from GraphBatchBook order by id", Book.class )
							.setEntityGraph( graph, GraphSemantic.FETCH )
							.getResultList();

			assertFetched( books );
		} );

		assertSelectCount( inspector, "GraphBatchBatchedAuthor", 1 );
		assertSelectCount( inspector, "GraphBatchSingleAuthor", 3 );
		assertSelectCount( inspector, "GraphBatchBook_batchedTags", 1 );
		assertSelectCount( inspector, "GraphBatchBook_singleTags", 3 );
	}

	@Test
	void fetchAnnotationBatchSizeControlsAssociationBatching(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var books =
					session.createQuery( "from GraphBatchBook order by id", Book.class )
							.setEntityGraph( Book_._Book_with_batch_sizes, GraphSemantic.FETCH )
							.getResultList();

			assertFetched( books );
		} );

		assertSelectCount( inspector, "GraphBatchBatchedAuthor", 1 );
		assertSelectCount( inspector, "GraphBatchSingleAuthor", 3 );
		assertSelectCount( inspector, "GraphBatchBook_batchedTags", 1 );
		assertSelectCount( inspector, "GraphBatchBook_singleTags", 3 );
	}

	private static void assertFetched( java.util.List<Book> books ) {
		assertThat( books ).hasSize( 3 );
		for ( Book book : books ) {
			assertThat( isInitialized( book.batchedAuthor ) ).isTrue();
			assertThat( isInitialized( book.singleAuthor ) ).isTrue();
			assertThat( isInitialized( book.batchedTags ) ).isTrue();
			assertThat( isInitialized( book.singleTags ) ).isTrue();
		}
	}

	private static void assertSelectCount(
			SQLStatementInspector inspector,
			String tableName,
			int expectedSelectCount) {
		final var normalizedTableName = tableName.toLowerCase( Locale.ROOT );
		final var matchingSelects =
				inspector.getSqlQueries().stream()
						.map( sql -> sql.toLowerCase( Locale.ROOT ) )
						.filter( sql -> sql.startsWith( "select" ) )
						.filter( sql -> sql.contains( normalizedTableName ) )
						.toList();
		assertThat( matchingSelects ).hasSize( expectedSelectCount );
	}

	@Entity(name = "GraphBatchBook")
	@Table(name = "GraphBatchBook")
	@NamedEntityGraph(name = NAMED_GRAPH)
	static class Book {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@Fetch(
				graph = NAMED_GRAPH,
				type = FetchType.EAGER,
				batchSize = 3
		)
		private BatchedAuthor batchedAuthor;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@Fetch(
				graph = NAMED_GRAPH,
				type = FetchType.EAGER,
				batchSize = 1
		)
		private SingleAuthor singleAuthor;

		@ElementCollection(fetch = FetchType.LAZY)
		@CollectionTable(name = "GraphBatchBook_batchedTags")
		@Fetch(
				graph = NAMED_GRAPH,
				type = FetchType.EAGER,
				batchSize = 3
		)
		private Set<String> batchedTags = new HashSet<>();

		@ElementCollection(fetch = FetchType.LAZY)
		@CollectionTable(name = "GraphBatchBook_singleTags")
		@Fetch(
				graph = NAMED_GRAPH,
				type = FetchType.EAGER,
				batchSize = 1
		)
		private Set<String> singleTags = new HashSet<>();

		Book() {
		}

		Book(Long id, BatchedAuthor batchedAuthor, SingleAuthor singleAuthor) {
			this.id = id;
			this.batchedAuthor = batchedAuthor;
			this.singleAuthor = singleAuthor;
			batchedTags.add( "batched-" + id );
			singleTags.add( "single-" + id );
		}
	}

	@Entity(name = "GraphBatchBatchedAuthor")
	@Table(name = "GraphBatchBatchedAuthor")
	static class BatchedAuthor {
		@Id
		private Long id;

		BatchedAuthor() {
		}

		BatchedAuthor(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "GraphBatchSingleAuthor")
	@Table(name = "GraphBatchSingleAuthor")
	static class SingleAuthor {
		@Id
		private Long id;

		SingleAuthor() {
		}

		SingleAuthor(Long id) {
			this.id = id;
		}
	}
}
