/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Fetch;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.Subgraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.Hibernate.isInitialized;

@Jpa(
		annotatedClasses = {
				JpaFetchAnnotationJpaApiTest.NoGraphBook.class,
				JpaFetchAnnotationJpaApiTest.GraphBook.class,
				JpaFetchAnnotationJpaApiTest.GraphDocument.class,
				JpaFetchAnnotationJpaApiTest.DistributedRoot.class,
				JpaFetchAnnotationJpaApiTest.DistributedAssociated.class,
				JpaFetchAnnotationJpaApiTest.DifferentlyNamedSubgraphRoot.class,
				JpaFetchAnnotationJpaApiTest.DifferentlyNamedSubgraphAssociated.class,
				JpaFetchAnnotationJpaApiTest.Publisher.class,
				JpaFetchAnnotationJpaApiTest.Department.class,
				JpaFetchAnnotationJpaApiTest.Reviewer.class
		}
)
class JpaFetchAnnotationJpaApiTest {
	private static final String BOOK_GRAPH = "JpaFetchApi.Book";
	private static final String DOCUMENT_GRAPH = "JpaFetchApi.Document";
	private static final String DISTRIBUTED_GRAPH = "JpaFetchApi.Distributed";
	private static final String DIFFERENTLY_NAMED_ROOT_GRAPH = "JpaFetchApi.DifferentlyNamedRoot";
	private static final String DIFFERENTLY_NAMED_ASSOCIATED_SUBGRAPH = "JpaFetchApi.DifferentlyNamedAssociated";
	private static final String RELATED_DETAILS = "relatedDetails";

	@BeforeEach
	void prepareData(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			final var fetchedPublisher = new Publisher( 1L );
			final var lazyPublisher = new Publisher( 2L );
			final var department = new Department( 1L );
			final var distributedDepartment = new Department( 2L );
			final var ignoredDepartment = new Department( 3L );
			final var differentlyNamedSubgraphDepartment = new Department( 4L );
			final var differentlyNamedIgnoredDepartment = new Department( 5L );
			final var author = new Reviewer( 1L );
			final var rootReviewer = new Reviewer( 2L );
			final var relatedReviewer = new Reviewer( 3L );
			final var ignoredReviewer = new Reviewer( 4L );

			final var noGraphBook = new NoGraphBook( 1L, fetchedPublisher, lazyPublisher );
			noGraphBook.tags.add( "annotations" );
			final var graphBook = new GraphBook( 1L, department, lazyPublisher );
			final var related = new GraphDocument( 2L, author, null, relatedReviewer, ignoredReviewer );
			final var document = new GraphDocument( 1L, author, related, rootReviewer, ignoredReviewer );
			final var distributedAssociated = new DistributedAssociated( 1L, distributedDepartment, lazyPublisher );
			final var distributedRoot = new DistributedRoot( 1L, distributedAssociated, ignoredDepartment );
			final var differentlyNamedAssociated =
					new DifferentlyNamedSubgraphAssociated( 1L, differentlyNamedSubgraphDepartment, lazyPublisher );
			final var differentlyNamedRoot =
					new DifferentlyNamedSubgraphRoot( 1L, differentlyNamedAssociated, differentlyNamedIgnoredDepartment );

			session.persist( fetchedPublisher );
			session.persist( lazyPublisher );
			session.persist( department );
			session.persist( distributedDepartment );
			session.persist( ignoredDepartment );
			session.persist( differentlyNamedSubgraphDepartment );
			session.persist( differentlyNamedIgnoredDepartment );
			session.persist( author );
			session.persist( rootReviewer );
			session.persist( relatedReviewer );
			session.persist( ignoredReviewer );
			session.persist( noGraphBook );
			session.persist( graphBook );
			session.persist( related );
			session.persist( document );
			session.persist( distributedAssociated );
			session.persist( distributedRoot );
			session.persist( differentlyNamedAssociated );
			session.persist( differentlyNamedRoot );
		} );
	}

	@AfterEach
	void cleanUp(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void fetchWithoutNamedEntityGraphAppliesToOrdinaryLoading(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = session.get( NoGraphBook.class, 1L );
			assertThat( isInitialized( book.fetchedPublisher ) ).isTrue();
			assertThat( isInitialized( book.tags ) ).isTrue();
			assertThat( isInitialized( book.lazyPublisher ) ).isFalse();
		} );
	}

	@Test
	void fetchWithNamedEntityGraphAppliesThroughJpaFind(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = session.get( JpaFetchAnnotationJpaApiTest_.GraphBook_._JpaFetchApi_Book, 1L );
			assertThat( isInitialized( book.department ) ).isTrue();
			assertThat( isInitialized( book.publisher ) ).isFalse();
		} );
	}

	@Test
	void fetchWithNamedEntityGraphAppliesThroughJpaQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book =
					session.createQuery( "from JpaFetchApiGraphBook where id = 1",
									JpaFetchAnnotationJpaApiTest_.GraphBook_._JpaFetchApi_Book )
							.getSingleResult();
			assertThat( isInitialized( book.department ) ).isTrue();
			assertThat( isInitialized( book.publisher ) ).isFalse();
		} );
	}

	@Test
	void fetchWithNamedSubgraphContributesSubgraphAttributeNode() {
		final var graph = JpaFetchAnnotationJpaApiTest_.GraphDocument_._JpaFetchApi_Document;
		assertThat( graph.getAttributeNodes() )
				.extracting( AttributeNode::getAttributeName )
				.contains( "author", "related" )
				.doesNotContain( "reviewer" );

		final var relatedSubgraph = valueSubgraph( graph.getAttributeNode( "related" ) );
		assertThat( relatedSubgraph.getAttributeNodes() )
				.extracting( AttributeNode::getAttributeName )
				.contains( "id", "reviewer" )
				.doesNotContain( "ignoredReviewer" );
	}

	@Test
	void fetchWithNamedSubgraphAppliesThroughJpaFind(EntityManagerFactoryScope scope) {
		final var graph = JpaFetchAnnotationJpaApiTest_.GraphDocument_._JpaFetchApi_Document;
		scope.inTransaction( session -> {
			final var document = session.get( graph, 1L );
			assertThat( isInitialized( document.author ) ).isTrue();
			assertThat( isInitialized( document.related ) ).isTrue();
			assertThat( isInitialized( document.reviewer ) ).isFalse();
			assertThat( isInitialized( document.related.reviewer ) ).isTrue();
			assertThat( isInitialized( document.related.ignoredReviewer ) ).isFalse();
		} );
	}

	@Test
	void fetchWithSubgraphDeclaredByAssociatedEntityContributesSubgraphAttributeNode() {
		final var graph = JpaFetchAnnotationJpaApiTest_.DistributedRoot_._JpaFetchApi_Distributed;

		assertThat( graph.getAttributeNodes() )
				.extracting( AttributeNode::getAttributeName )
				.contains( "associated" )
				.doesNotContain( "ignoredDepartment" );

		final var associatedSubgraph = valueSubgraph( graph.getAttributeNode( "associated" ) );
		assertThat( associatedSubgraph.getAttributeNodes() )
				.extracting( AttributeNode::getAttributeName )
				.contains( "department" )
				.doesNotContain( "ignoredPublisher" );
	}

	@Test
	void fetchWithSubgraphDeclaredByAssociatedEntityAppliesThroughJpaFind(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			final var root = session.get( JpaFetchAnnotationJpaApiTest_.DistributedRoot_._JpaFetchApi_Distributed, 1L );
			assertThat( isInitialized( root.associated ) ).isTrue();
			assertThat( isInitialized( root.ignoredDepartment ) ).isFalse();
			assertThat( isInitialized( root.associated.department ) ).isTrue();
			assertThat( isInitialized( root.associated.ignoredPublisher ) ).isFalse();
		} );
	}

	@Test
	void fetchWithDifferentlyNamedAssociatedEntitySubgraphContributesSubgraphAttributeNode() {
		final var graph =
				JpaFetchAnnotationJpaApiTest_.DifferentlyNamedSubgraphRoot_._JpaFetchApi_DifferentlyNamedRoot;

		assertThat( graph.getAttributeNodes() )
				.extracting( AttributeNode::getAttributeName )
				.contains( "associated" )
				.doesNotContain( "ignoredDepartment" );

		final var associatedSubgraph = valueSubgraph( graph.getAttributeNode( "associated" ) );
		assertThat( associatedSubgraph.getAttributeNodes() )
				.extracting( AttributeNode::getAttributeName )
				.contains( "department" )
				.doesNotContain( "ignoredPublisher" );
	}

	@Test
	void fetchWithDifferentlyNamedAssociatedEntitySubgraphAppliesThroughJpaFind(EntityManagerFactoryScope scope) {
		final var graph =
				JpaFetchAnnotationJpaApiTest_.DifferentlyNamedSubgraphRoot_._JpaFetchApi_DifferentlyNamedRoot;

		scope.inTransaction( session -> {
			final var root = session.get( graph, 1L );

			assertThat( isInitialized( root.associated ) ).isTrue();
			assertThat( isInitialized( root.ignoredDepartment ) ).isFalse();
			assertThat( isInitialized( root.associated.department ) ).isTrue();
			assertThat( isInitialized( root.associated.ignoredPublisher ) ).isFalse();
		} );
	}

	private static Subgraph<?> valueSubgraph(AttributeNode<?> node) {
		assertThat( node.getSubgraphs() ).hasSize( 1 );
		return node.getSubgraphs().values().iterator().next();
	}

	@Entity(name = "JpaFetchApiNoGraphBook")
	static class NoGraphBook {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch
		private Publisher fetchedPublisher;

		@ManyToOne(fetch = FetchType.LAZY)
		private Publisher lazyPublisher;

		@ElementCollection(fetch = FetchType.LAZY)
		@Fetch
		private Set<String> tags = new HashSet<>();

		NoGraphBook() {
		}

		NoGraphBook(Long id, Publisher fetchedPublisher, Publisher lazyPublisher) {
			this.id = id;
			this.fetchedPublisher = fetchedPublisher;
			this.lazyPublisher = lazyPublisher;
		}
	}

	@Entity(name = "JpaFetchApiGraphBook")
	@NamedEntityGraph(name = BOOK_GRAPH)
	static class GraphBook {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(graph = BOOK_GRAPH)
		private Department department;

		@ManyToOne(fetch = FetchType.LAZY)
		private Publisher publisher;

		GraphBook() {
		}

		GraphBook(Long id, Department department, Publisher publisher) {
			this.id = id;
			this.department = department;
			this.publisher = publisher;
		}
	}

	@Entity(name = "JpaFetchApiGraphDocument")
	@NamedEntityGraph(
			name = DOCUMENT_GRAPH,
			attributeNodes = @NamedAttributeNode(value = "related", subgraph = RELATED_DETAILS),
			subgraphs = @NamedSubgraph(
					name = RELATED_DETAILS,
					type = GraphDocument.class,
					attributeNodes = @NamedAttributeNode("id")
			)
	)
	static class GraphDocument {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(graph = DOCUMENT_GRAPH)
		private Reviewer author;

		@ManyToOne(fetch = FetchType.LAZY)
		private GraphDocument related;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(graph = DOCUMENT_GRAPH, subgraph = RELATED_DETAILS)
		private Reviewer reviewer;

		@ManyToOne(fetch = FetchType.LAZY)
		private Reviewer ignoredReviewer;

		GraphDocument() {
		}

		GraphDocument(
				Long id,
				Reviewer author,
				GraphDocument related,
				Reviewer reviewer,
				Reviewer ignoredReviewer) {
			this.id = id;
			this.author = author;
			this.related = related;
			this.reviewer = reviewer;
			this.ignoredReviewer = ignoredReviewer;
		}
	}

	@Entity(name = "JpaFetchApiDistributedRoot")
	@NamedEntityGraph(name = DISTRIBUTED_GRAPH)
	static class DistributedRoot {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(graph = DISTRIBUTED_GRAPH, subgraph = DISTRIBUTED_GRAPH)
		private DistributedAssociated associated;

		@ManyToOne(fetch = FetchType.LAZY)
		private Department ignoredDepartment;

		DistributedRoot() {
		}

		DistributedRoot(Long id, DistributedAssociated associated, Department ignoredDepartment) {
			this.id = id;
			this.associated = associated;
			this.ignoredDepartment = ignoredDepartment;
		}
	}

	@Entity(name = "JpaFetchApiDistributedAssociated")
	@NamedEntityGraph(name = DISTRIBUTED_GRAPH)
	static class DistributedAssociated {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(graph = DISTRIBUTED_GRAPH)
		private Department department;

		@ManyToOne(fetch = FetchType.LAZY)
		private Publisher ignoredPublisher;

		DistributedAssociated() {
		}

		DistributedAssociated(Long id, Department department, Publisher ignoredPublisher) {
			this.id = id;
			this.department = department;
			this.ignoredPublisher = ignoredPublisher;
		}
	}

	@Entity(name = "JpaFetchApiDifferentlyNamedSubgraphRoot")
	@NamedEntityGraph(name = DIFFERENTLY_NAMED_ROOT_GRAPH)
	static class DifferentlyNamedSubgraphRoot {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(graph = DIFFERENTLY_NAMED_ROOT_GRAPH, subgraph = DIFFERENTLY_NAMED_ASSOCIATED_SUBGRAPH)
		private DifferentlyNamedSubgraphAssociated associated;

		@ManyToOne(fetch = FetchType.LAZY)
		private Department ignoredDepartment;

		DifferentlyNamedSubgraphRoot() {
		}

		DifferentlyNamedSubgraphRoot(
				Long id,
				DifferentlyNamedSubgraphAssociated associated,
				Department ignoredDepartment) {
			this.id = id;
			this.associated = associated;
			this.ignoredDepartment = ignoredDepartment;
		}
	}

	@Entity(name = "JpaFetchApiDifferentlyNamedSubgraphAssociated")
	@NamedEntityGraph(name = DIFFERENTLY_NAMED_ASSOCIATED_SUBGRAPH)
	static class DifferentlyNamedSubgraphAssociated {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(graph = DIFFERENTLY_NAMED_ASSOCIATED_SUBGRAPH)
		private Department department;

		@ManyToOne(fetch = FetchType.LAZY)
		private Publisher ignoredPublisher;

		DifferentlyNamedSubgraphAssociated() {
		}

		DifferentlyNamedSubgraphAssociated(Long id, Department department, Publisher ignoredPublisher) {
			this.id = id;
			this.department = department;
			this.ignoredPublisher = ignoredPublisher;
		}
	}

	@Entity(name = "JpaFetchApiPublisher")
	static class Publisher {
		@Id
		private Long id;

		Publisher() {
		}

		Publisher(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "JpaFetchApiDepartment")
	static class Department {
		@Id
		private Long id;

		Department() {
		}

		Department(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "JpaFetchApiReviewer")
	static class Reviewer {
		@Id
		private Long id;

		Reviewer() {
		}

		Reviewer(Long id) {
			this.id = id;
		}
	}
}
