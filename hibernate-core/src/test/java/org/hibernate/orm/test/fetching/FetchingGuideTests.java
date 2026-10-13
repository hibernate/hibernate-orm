package org.hibernate.orm.test.fetching;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Executable examples for applying and inspecting User Guide fetch plans.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = { FetchingGuideTests.Book.class, FetchingGuideTests.Person.class })
@SessionFactory
public class FetchingGuideTests {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person author = new Person( 1L, "Author" );
			Person reviewer = new Person( 2L, "Reviewer" );
			session.persist( author );
			session.persist( reviewer );
			session.persist( new Book( 1L, "First", author, reviewer ) );
			session.persist( new Book( 2L, "Second", author, reviewer ) );
		} );
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void standardGraphFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::fetching-guide-find[]
			EntityGraph<Book> graph = entityManager.createEntityGraph( Book.class );
			graph.addAttributeNode( "author" );
			Book book = entityManager.find( graph, 1L, GraphSemantic.FETCH );
			//end::fetching-guide-find[]
			assertThat( Hibernate.isInitialized( book.author ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.reviewer ) ).isFalse();
			assertThat( book.author.getName() ).isEqualTo( "Author" );
		} );
	}

	@Test
	void standardGraphMultiple(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			EntityGraph<Book> graph = entityManager.createEntityGraph( Book.class );
			graph.addAttributeNode( "author" );
			//tag::fetching-guide-multiple[]
			List<Book> books = entityManager.findMultiple( graph, List.of( 1L, 2L ) );
			//end::fetching-guide-multiple[]
			assertThat( books ).hasSize( 2 );
			for ( Book book : books ) {
				assertThat( Hibernate.isInitialized( book.author ) ).isTrue();
				assertThat( Hibernate.isInitialized( book.reviewer ) ).isTrue();
			}
		} );
	}

	@Test
	void standardGraphQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			EntityGraph<Book> graph = entityManager.createEntityGraph( Book.class );
			graph.addAttributeNode( "author" );
			//tag::fetching-guide-query[]
			List<Book> books = entityManager.createQuery(
					"from FetchGuideBook b order by b.id", graph )
					.getResultList();
			//end::fetching-guide-query[]
			assertThat( books ).hasSize( 2 );
			assertThat( Hibernate.isInitialized( books.get( 0 ).author ) ).isTrue();
		} );
	}

	@Test
	void loadGraphRetainsEagerMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			EntityGraph<Book> graph = entityManager.createEntityGraph( Book.class );
			graph.addAttributeNode( "author" );
			//tag::fetching-guide-load-hint[]
			Book book = entityManager.createQuery( "from FetchGuideBook where id = :id", Book.class )
					.setParameter( "id", 1L )
					.setHint( "jakarta.persistence.loadgraph", graph )
					.getSingleResult();
			//end::fetching-guide-load-hint[]
			assertThat( Hibernate.isInitialized( book.author ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.reviewer ) ).isTrue();
		} );
	}

	@Test
	void removeNodeOverridesEagerMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::fetching-guide-removed-node[]
			EntityGraph<Book> graph = entityManager.createEntityGraph( Book.class );
			graph.addAttributeNode( "author" );
			graph.removeAttributeNode( "reviewer" );
			Book book = entityManager.find( graph, 1L, GraphSemantic.LOAD );
			//end::fetching-guide-removed-node[]
			assertThat( Hibernate.isInitialized( book.author ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.reviewer ) ).isFalse();
		} );
	}

	@Test
	void explicitLazyGraphNode(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::fetching-guide-lazy-node[]
			EntityGraph<Book> graph = entityManager.createEntityGraph( Book.class );
			graph.addAttributeNode( "reviewer" ).addOption( FetchType.LAZY );
			Book book = entityManager.find( graph, 1L, GraphSemantic.LOAD );
			//end::fetching-guide-lazy-node[]
			assertThat( Hibernate.isInitialized( book.reviewer ) ).isFalse();
		} );
	}

	@Test
	void declarativeGraphOverride(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::fetching-guide-named-graph[]
			EntityGraph<Book> graph = entityManager.getEntityGraph( Book.class, "Book.detail" );
			Book book = entityManager.find( graph, 1L, GraphSemantic.LOAD );
			//end::fetching-guide-named-graph[]
			assertThat( Hibernate.isInitialized( book.author ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.reviewer ) ).isFalse();
		} );
	}

	@Test
	void standardExplicitLoading(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			//tag::fetching-guide-initialization[]
			Book book = entityManager.find( Book.class, 1L );
			var util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
			boolean authorLoaded = util.isLoaded( book, "author" );
			util.load( book.author );
			//end::fetching-guide-initialization[]
			assertThat( authorLoaded ).isFalse();
			assertThat( util.isLoaded( book, "author" ) ).isTrue();
			assertThat( book.author.getName() ).isEqualTo( "Author" );
		} );
	}

	@Test
	void unloadedStateAfterDetachment(SessionFactoryScope scope) {
		Book detached = scope.fromTransaction( session -> session.find( Book.class, 1L ) );
		assertThat( Hibernate.isInitialized( detached.author ) ).isFalse();
		assertThrows( LazyInitializationException.class, () -> Hibernate.initialize( detached.author ) );
	}

	@Test
	void graphDoesNotUnloadExistingState(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session;
			Book existing = entityManager.find( Book.class, 1L );
			assertThat( Hibernate.isInitialized( existing.reviewer ) ).isTrue();
			EntityGraph<Book> graph = entityManager.createEntityGraph( Book.class );
			graph.removeAttributeNode( "reviewer" );
			Book same = entityManager.find( graph, 1L, GraphSemantic.FETCH );
			assertThat( same ).isSameAs( existing );
			assertThat( Hibernate.isInitialized( same.reviewer ) ).isTrue();
		} );
	}

	//tag::fetching-guide-graph-mapping[]
	@Entity(name = "FetchGuideBook")
	@NamedEntityGraph(name = "Book.detail", attributeNodes = @NamedAttributeNode("author"))
	@Table(name = "fetch_guide_book")
	public static class Book {
		@Id
		Long id;
		String title;

		@ManyToOne(fetch = FetchType.LAZY)
		Person author;

		@ManyToOne(fetch = FetchType.EAGER)
		@jakarta.persistence.Fetch(graph = "Book.detail", type = FetchType.LAZY)
		Person reviewer;
	//end::fetching-guide-graph-mapping[]

		Book() {
		}

		Book(Long id, String title, Person author, Person reviewer) {
			this.id = id;
			this.title = title;
			this.author = author;
			this.reviewer = reviewer;
		}
	//tag::fetching-guide-graph-mapping[]
	}
	//end::fetching-guide-graph-mapping[]

	@Entity(name = "FetchGuidePerson")
	@Table(name = "fetch_guide_person")
	public static class Person {
		@Id
		Long id;
		String name;

		Person() {
		}

		public String getName() {
			return name;
		}

		Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
