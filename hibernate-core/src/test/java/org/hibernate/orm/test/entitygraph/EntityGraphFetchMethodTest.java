/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.FetchMethod;
import org.hibernate.Hibernate;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.orm.test.entitygraph.EntityGraphFetchMethodTest_.Author_;
import org.hibernate.orm.test.entitygraph.EntityGraphFetchMethodTest_.Book_;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				EntityGraphFetchMethodTest.Book.class,
				EntityGraphFetchMethodTest.Author.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
class EntityGraphFetchMethodTest {

	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var author = new Author( 1L, "Gavin" );
			final var secondAuthor = new Author( 2L, "Steve" );
			session.persist( author );
			session.persist( secondAuthor );
			session.persist( new Book( 1L, "Hibernate in Action", author ) );
			session.persist( new Book( 2L, "Hibernate Reactive", secondAuthor ) );
		} );
		scope.getCollectingStatementInspector().clear();
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void findWithGraphFetchMethodSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Book.class );
			graph.addAttributeNode( Book_.author )
					.addOption( FetchMethod.BY_ID );
			final var book = session.find( graph, 1L );

			assertThat( book ).isNotNull();
			assertThat( Hibernate.isInitialized( book.author ) ).isTrue();
			assertThat( book.author.name ).isEqualTo( "Gavin" );
		} );

		inspector.assertExecutedCount( 2 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
	}

	@Test
	void getWithGraphFetchMethodSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Book.class );
			graph.addAttributeNode( Book_.author )
					.addOption( FetchMethod.BY_ID );
			final var book = session.get( graph, 1L );

			assertThat( Hibernate.isInitialized( book.author ) ).isTrue();
			assertThat( book.author.name ).isEqualTo( "Gavin" );
		} );

		inspector.assertExecutedCount( 2 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
	}

	@Test
	void findWithGraphFetchMethodJoin(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Book.class );
			graph.addAttributeNode( Book_.author )
					.addOption( FetchMethod.JOIN );
			final var book = session.find( graph, 1L );

			assertThat( book ).isNotNull();
			assertThat( Hibernate.isInitialized( book.author ) ).isTrue();
			assertThat( book.author.name ).isEqualTo( "Gavin" );
		} );

		inspector.assertExecutedCount( 1 );
		inspector.assertNumberOfJoins( 0, 1 );
	}

	@Test
	void getWithGraphFetchMethodJoin(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Book.class );
			graph.addAttributeNode( Book_.author )
					.addOption( FetchMethod.JOIN );
			final var book = session.get( graph, 1L );

			assertThat( Hibernate.isInitialized( book.author ) ).isTrue();
			assertThat( book.author.name ).isEqualTo( "Gavin" );
		} );

		inspector.assertExecutedCount( 1 );
		inspector.assertNumberOfJoins( 0, 1 );
	}

	@Test
	void findWithOneToManyGraphFetchMethodSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books )
					.addOption( FetchMethod.BY_ID );
			final var author = session.find( graph, 1L );

			assertThat( author ).isNotNull();
			assertThat( Hibernate.isInitialized( author.books ) ).isTrue();
			assertThat( author.books )
					.extracting( book -> book.title )
					.containsExactly( "Hibernate in Action" );
		} );

		inspector.assertExecutedCount( 2 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
	}

	@Test
	void getWithOneToManyGraphFetchMethodSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books )
					.addOption( FetchMethod.BY_ID );
			final var author = session.get( graph, 1L );

			assertThat( Hibernate.isInitialized( author.books ) ).isTrue();
			assertThat( author.books )
					.extracting( book -> book.title )
					.containsExactly( "Hibernate in Action" );
		} );

		inspector.assertExecutedCount( 2 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
	}

	@Test
	void findWithOneToManyGraphFetchMethodJoin(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books )
					.addOption( FetchMethod.JOIN );
			final var author = session.find( graph, 1L );

			assertThat( author ).isNotNull();
			assertThat( Hibernate.isInitialized( author.books ) ).isTrue();
			assertThat( author.books )
					.extracting( book -> book.title )
					.containsExactly( "Hibernate in Action" );
		} );

		inspector.assertExecutedCount( 1 );
		inspector.assertNumberOfJoins( 0, 1 );
	}

	@Test
	void getWithOneToManyGraphFetchMethodJoin(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books )
					.addOption( FetchMethod.JOIN );
			final var author = session.get( graph, 1L );

			assertThat( Hibernate.isInitialized( author.books ) ).isTrue();
			assertThat( author.books )
					.extracting( book -> book.title )
					.containsExactly( "Hibernate in Action" );
		} );

		inspector.assertExecutedCount( 1 );
		inspector.assertNumberOfJoins( 0, 1 );
	}

	@Test
	void queryWithOneToManyGraphFetchMethodSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books )
					.addOption( FetchMethod.BY_ID );
			final var authors =
					session.createQuery("from GraphFetchMethodAuthor a order by a.id" )
							.withEntityGraph( graph )
							.getResultList();

			assertAuthors( authors );
		} );

		inspector.assertExecutedCount( 3 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
		inspector.assertNumberOfJoins( 2, 0 );
		assertThat( inspector.getSqlQueries().get( 1 ).toLowerCase( Locale.ROOT ) )
				.doesNotContain( " in (select " );
		assertThat( inspector.getSqlQueries().get( 2 ).toLowerCase( Locale.ROOT ) )
				.doesNotContain( " in (select " );
	}

	@Test
	void queryWithOneToManyGraphFetchMethodJoin(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books )
					.addOption( FetchMethod.JOIN );
			final var authors =
					session.createQuery("from GraphFetchMethodAuthor a order by a.id")
							.withEntityGraph( graph )
							.getResultList();

			assertAuthors( authors );
		} );

		inspector.assertExecutedCount( 1 );
		inspector.assertNumberOfJoins( 0, 1 );
	}

	@Test
	void queryWithOneToManyGraphFetchMethodBulkSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books )
					.addOption( FetchMethod.BY_SUBQUERY );
			final var authors =
					session.createQuery("from GraphFetchMethodAuthor a order by a.id")
							.withEntityGraph( graph )
							.getResultList();

			assertAuthors( authors );
		} );

		inspector.assertExecutedCount( 2 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
		assertThat( inspector.getSqlQueries().get( 1 ).toLowerCase( Locale.ROOT ) )
				.contains( " in (select " );
	}

	@Test
	void criteriaQueryWithOneToManyGraphFetchMethodSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books )
					.addOption( FetchMethod.BY_ID );
			final var criteriaBuilder = session.getCriteriaBuilder();
			final var criteriaQuery = criteriaBuilder.createQuery( Author.class );
			final var author = criteriaQuery.from( Author.class );
			criteriaQuery.select( author )
					.orderBy( criteriaBuilder.asc( author.get( Author_.id ) ) );

			final var authors = session.createQuery( criteriaQuery )
					.setEntityGraph( graph, GraphSemantic.FETCH )
					.getResultList();

			assertAuthors( authors );
		} );

		inspector.assertExecutedCount( 3 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
		inspector.assertNumberOfJoins( 2, 0 );
		assertThat( inspector.getSqlQueries().get( 1 ).toLowerCase( Locale.ROOT ) )
				.doesNotContain( " in (select " );
		assertThat( inspector.getSqlQueries().get( 2 ).toLowerCase( Locale.ROOT ) )
				.doesNotContain( " in (select " );
	}

	@Test
	void criteriaQueryWithOneToManyGraphFetchMethodBulkSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Author.class );
			graph.addAttributeNode( Author_.books )
					.addOption( FetchMethod.BY_SUBQUERY );
			final var criteriaBuilder = session.getCriteriaBuilder();
			final var criteriaQuery = criteriaBuilder.createQuery( Author.class );
			final var author = criteriaQuery.from( Author.class );
			criteriaQuery.select( author )
					.orderBy( criteriaBuilder.asc( author.get( Author_.id ) ) );

			final var authors = session.createQuery( criteriaQuery )
					.setEntityGraph( graph, GraphSemantic.FETCH )
					.getResultList();

			assertAuthors( authors );
		} );

		inspector.assertExecutedCount( 2 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
		assertThat( inspector.getSqlQueries().get( 1 ).toLowerCase( Locale.ROOT ) )
				.contains( " in (select " );
	}

	@Test
	void queryWithToOneGraphFetchMethodSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Book.class );
			graph.addAttributeNode( Book_.author )
					.addOption( FetchMethod.BY_ID );
			final var books =
					session.createQuery("from GraphFetchMethodBook b order by b.id")
							.withEntityGraph( graph )
							.getResultList();

			assertBooks( books );
		} );

		inspector.assertExecutedCount( 3 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
		inspector.assertNumberOfJoins( 2, 0 );
		assertThat( inspector.getSqlQueries().get( 1 ).toLowerCase( Locale.ROOT ) )
				.doesNotContain( " in (select " );
		assertThat( inspector.getSqlQueries().get( 2 ).toLowerCase( Locale.ROOT ) )
				.doesNotContain( " in (select " );
	}

	@Test
	void queryWithToOneGraphFetchMethodBulkSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Book.class );
			graph.addAttributeNode( Book_.author )
					.addOption( FetchMethod.BY_SUBQUERY );
			final var books =
					session.createQuery("from GraphFetchMethodBook b order by b.id")
							.withEntityGraph( graph )
							.getResultList();

			assertBooks( books );
		} );

		inspector.assertExecutedCount( 2 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
		assertThat( inspector.getSqlQueries().get( 1 ).toLowerCase( Locale.ROOT ) )
				.contains( " in (select " );
	}

	@Test
	void criteriaQueryWithToOneGraphFetchMethodSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Book.class );
			graph.addAttributeNode( Book_.author )
					.addOption( FetchMethod.BY_ID );
			final var criteriaBuilder = session.getCriteriaBuilder();
			final var criteriaQuery = criteriaBuilder.createQuery( Book.class );
			final var book = criteriaQuery.from( Book.class );
			criteriaQuery.select( book )
					.orderBy( criteriaBuilder.asc( book.get( Book_.id ) ) );

			final var books = session.createQuery( criteriaQuery )
					.setEntityGraph( graph, GraphSemantic.FETCH )
					.getResultList();

			assertBooks( books );
		} );

		inspector.assertExecutedCount( 3 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
		inspector.assertNumberOfJoins( 2, 0 );
		assertThat( inspector.getSqlQueries().get( 1 ).toLowerCase( Locale.ROOT ) )
				.doesNotContain( " in (select " );
		assertThat( inspector.getSqlQueries().get( 2 ).toLowerCase( Locale.ROOT ) )
				.doesNotContain( " in (select " );
	}

	@Test
	void criteriaQueryWithToOneGraphFetchMethodBulkSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Book.class );
			graph.addAttributeNode( Book_.author )
					.addOption( FetchMethod.BY_SUBQUERY );
			final var criteriaBuilder = session.getCriteriaBuilder();
			final var criteriaQuery = criteriaBuilder.createQuery( Book.class );
			final var book = criteriaQuery.from( Book.class );
			criteriaQuery.select( book )
					.orderBy( criteriaBuilder.asc( book.get( Book_.id ) ) );

			final var books = session.createQuery( criteriaQuery )
					.setEntityGraph( graph, GraphSemantic.FETCH )
					.getResultList();

			assertBooks( books );
		} );

		inspector.assertExecutedCount( 2 );
		inspector.assertNumberOfJoins( 0, 0 );
		inspector.assertNumberOfJoins( 1, 0 );
		assertThat( inspector.getSqlQueries().get( 1 ).toLowerCase( Locale.ROOT ) )
				.contains( " in (select " );
	}

	private static void assertAuthors(List<Author> authors) {
		assertThat( authors ).hasSize( 2 );
		assertThat( Hibernate.isInitialized( authors.get( 0 ).books ) ).isTrue();
		assertThat( authors.get( 0 ).books )
				.extracting( book -> book.title )
				.containsExactly( "Hibernate in Action" );
		assertThat( Hibernate.isInitialized( authors.get( 1 ).books ) ).isTrue();
		assertThat( authors.get( 1 ).books )
				.extracting( book -> book.title )
				.containsExactly( "Hibernate Reactive" );
	}

	private static void assertBooks(List<Book> books) {
		assertThat( books ).hasSize( 2 );
		assertThat( books.get( 0 ).title ).isEqualTo( "Hibernate in Action" );
		assertThat( Hibernate.isInitialized( books.get( 0 ).author ) ).isTrue();
		assertThat( books.get( 0 ).author.name ).isEqualTo( "Gavin" );
		assertThat( books.get( 1 ).title ).isEqualTo( "Hibernate Reactive" );
		assertThat( Hibernate.isInitialized( books.get( 1 ).author ) ).isTrue();
		assertThat( books.get( 1 ).author.name ).isEqualTo( "Steve" );
	}

	@Entity(name = "GraphFetchMethodBook")
	static class Book {
		@Id
		private Long id;

		private String title;

		@ManyToOne(fetch = FetchType.LAZY,
				cascade = CascadeType.PERSIST)
		private Author author;

		Book() {
		}

		Book(Long id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}
	}

	@Entity(name = "GraphFetchMethodAuthor")
	static class Author {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = Book_.AUTHOR)
		private Set<Book> books;

		Author() {
		}

		Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
