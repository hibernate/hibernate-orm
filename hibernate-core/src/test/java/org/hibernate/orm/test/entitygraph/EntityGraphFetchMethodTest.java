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
import org.hibernate.orm.test.entitygraph.EntityGraphFetchMethodTest_.Author_;
import org.hibernate.orm.test.entitygraph.EntityGraphFetchMethodTest_.Book_;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
			session.persist( author );
			session.persist( new Book( 1L, "Hibernate in Action", author ) );
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
					.addOption( FetchMethod.SELECT );
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
					.addOption( FetchMethod.SELECT );
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
					.addOption( FetchMethod.SELECT );
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
					.addOption( FetchMethod.SELECT );
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
