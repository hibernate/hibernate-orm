/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.Set;

import org.hibernate.AnnotationException;
import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Fetch;
import jakarta.persistence.FetchOption;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.QueryHint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(
		annotatedClasses = {
				JpaFetchAnnotationGraphTest.Book.class,
				JpaFetchAnnotationGraphTest.Author.class,
				JpaFetchAnnotationGraphTest.Publisher.class,
				JpaFetchAnnotationGraphTest.Editor.class,
				JpaFetchAnnotationGraphTest.Target.class
		}
)
@SessionFactory
class JpaFetchAnnotationGraphTest {
	private static final String DETAIL_GRAPH = "Book.detail";
	private static final String LAZY_GRAPH = "Book.lazy";
	private static final String RELATED_GRAPH = "Book.related";

	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var author = new Author( 1L );
			final var publisher = new Publisher( 1L );
			final var editor = new Editor( 1L );
			final var relatedPublisher = new Publisher( 2L );
			final var related = new Book( 2L, author, relatedPublisher, editor, null );
			final var book = new Book( 1L, author, publisher, editor, related );

			session.persist( author );
			session.persist( publisher );
			session.persist( editor );
			session.persist( relatedPublisher );
			session.persist( related );
			session.persist( book );
		} );
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> scope.getSessionFactory().getSchemaManager().truncate() );
	}

	@Test
	void contributesRootGraphFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = session.createQuery( "from FetchBook where id = 1", Book.class )
					.setEntityGraph( (EntityGraph<? super Book>) session.getEntityGraph( DETAIL_GRAPH ), GraphSemantic.FETCH )
					.getSingleResult();

			assertThat( Hibernate.isInitialized( book.author ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.publisher ) ).isFalse();
		} );
	}

	@Test
	void lazyFetchTypeOverridesExistingGraphNode(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = session.createQuery( "from FetchBook where id = 1", Book.class )
					.setEntityGraph( (EntityGraph<? super Book>) session.getEntityGraph( LAZY_GRAPH ), GraphSemantic.FETCH )
					.getSingleResult();

			assertThat( Hibernate.isInitialized( book.editor ) ).isFalse();
		} );
	}

	@Test
	void contributesNamedSubgraphFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = session.createQuery( "from FetchBook where id = 1", Book.class )
					.setEntityGraph( (EntityGraph<? super Book>) session.getEntityGraph( RELATED_GRAPH ), GraphSemantic.FETCH )
					.getSingleResult();

			assertThat( Hibernate.isInitialized( book.related ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.related.publisher ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.publisher ) ).isFalse();
		} );
	}

	@Test
	void storesFetchOptionsOnGraphNode(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var graph = (RootGraphImplementor<?>) session.getEntityGraph( DETAIL_GRAPH );
			final Set<FetchOption> options = graph.findNode( "author" ).getOptions();

			assertThat( options ).contains( FetchType.EAGER, CacheStoreMode.BYPASS, new jakarta.persistence.BatchSize( 16 ) );
			assertThat( options ).anySatisfy( option -> {
				assertThat( option.getClass().getSimpleName() ).isEqualTo( "FetchHintOptions" );
				assertThat( option.toString() ).contains( "sample.fetch.hint", "true" );
			} );
		} );
	}

	@Test
	@ServiceRegistry
	void unknownGraphNameFailsBoot(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> buildSessionFactory( registryScope, UnknownGraph.class, Target.class ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "unknown entity graph 'missing'" );
	}

	@Test
	@ServiceRegistry
	void unknownSubgraphNameFailsBoot(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> buildSessionFactory( registryScope, UnknownSubgraph.class, Target.class ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "unknown subgraph 'missing'" );
	}

	@Test
	@ServiceRegistry
	void conflictingFetchTypeFailsBoot(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> buildSessionFactory( registryScope, ConflictingFetchTypes.class, Target.class ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "conflicting '@Fetch' options" );
	}

	@Test
	@ServiceRegistry
	void conflictingOverlappingSubgraphFetchTypeFailsBoot(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> buildSessionFactory( registryScope, ConflictingOverlappingSubgraphs.class, Target.class ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "conflicting '@Fetch' options" );
	}

	private static void buildSessionFactory(ServiceRegistryScope registryScope, Class<?>... classes) {
		try (var sessionFactory = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( classes )
				.buildMetadata()
				.buildSessionFactory()) {
		}
	}

	@Entity(name = "FetchBook")
	@NamedEntityGraph(name = DETAIL_GRAPH)
	@NamedEntityGraph(name = LAZY_GRAPH, attributeNodes = @NamedAttributeNode("editor"))
	@NamedEntityGraph(
			name = RELATED_GRAPH,
			attributeNodes = @NamedAttributeNode(value = "related", subgraph = "bookDetails"),
			subgraphs = @NamedSubgraph(name = "bookDetails", attributeNodes = @NamedAttributeNode("id"))
	)
	static class Book {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@Fetch(
				graph = DETAIL_GRAPH,
				type = FetchType.EAGER,
				batchSize = 16,
				cacheStoreMode = CacheStoreMode.BYPASS,
				hints = @QueryHint(name = "sample.fetch.hint", value = "true")
		)
		private Author author;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@Fetch(graph = RELATED_GRAPH, subgraph = "bookDetails", type = FetchType.EAGER)
		private Publisher publisher;

		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
		@Fetch(graph = LAZY_GRAPH, type = FetchType.LAZY)
		private Editor editor;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Book related;

		Book() {
		}

		Book(Long id, Author author, Publisher publisher, Editor editor, Book related) {
			this.id = id;
			this.author = author;
			this.publisher = publisher;
			this.editor = editor;
			this.related = related;
		}
	}

	@Entity(name = "FetchAuthor")
	static class Author {
		@Id
		private Long id;

		Author() {
		}

		Author(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "FetchPublisher")
	static class Publisher {
		@Id
		private Long id;

		Publisher() {
		}

		Publisher(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "FetchEditor")
	static class Editor {
		@Id
		private Long id;

		Editor() {
		}

		Editor(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "FetchTarget")
	static class Target {
		@Id
		private Long id;
	}

	@Entity(name = "UnknownGraph")
	@NamedEntityGraph(name = "known")
	static class UnknownGraph {
		@Id
		private Long id;

		@ManyToOne
		@Fetch(graph = "missing")
		private Target target;
	}

	@Entity(name = "UnknownSubgraph")
	@NamedEntityGraph(name = "known", subgraphs = @NamedSubgraph(name = "knownSubgraph", attributeNodes = @NamedAttributeNode("id")))
	static class UnknownSubgraph {
		@Id
		private Long id;

		@ManyToOne
		@Fetch(graph = "known", subgraph = "missing")
		private Target target;
	}

	@Entity(name = "ConflictingFetchTypes")
	@NamedEntityGraph(name = "known")
	static class ConflictingFetchTypes {
		@Id
		private Long id;

		@ManyToOne
		@Fetch(graph = "known", type = FetchType.EAGER)
		@Fetch(graph = "known", type = FetchType.LAZY)
		private Target target;
	}

	@Entity(name = "ConflictingOverlappingSubgraphs")
	@NamedEntityGraph(
			name = "known",
			subgraphs = {
					@NamedSubgraph(name = "first", attributeNodes = @NamedAttributeNode("id")),
					@NamedSubgraph(name = "second", attributeNodes = @NamedAttributeNode("id"))
			}
	)
	static class ConflictingOverlappingSubgraphs {
		@Id
		private Long id;

		@ManyToOne
		@Fetch(graph = "known", subgraph = { "first", "second" }, type = FetchType.EAGER)
		@Fetch(graph = "known", subgraph = "second", type = FetchType.LAZY)
		private Target target;
	}
}
