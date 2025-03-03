/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = {
		LeftJoinNullnessPredicateQueryTest.Author.class,
		LeftJoinNullnessPredicateQueryTest.Book.class
})
@Jira( "https://hibernate.atlassian.net/browse/HHH-16505" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17379" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17397" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-19116" )
public class LeftJoinNullnessPredicateQueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author hpLovecraft = new Author( 1L, "Howard Phillips Lovecraft", false );
			final Author sKing = new Author( 2L, "Stephen King", true );
			session.persist( hpLovecraft );
			session.persist( sKing );
			session.persist( new Book( "The Shining", sKing ) );
			session.persist( new Book( "The Colour Out of Space", hpLovecraft ) );
			session.persist( new Book( "Unknown Author", null ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Book" ).executeUpdate();
			session.createMutationQuery( "delete from Author" ).executeUpdate();
		} );
	}

	private static void withInspector(SessionFactoryScope scope, BiConsumer<SessionImplementor, SQLStatementInspector> consumer) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> consumer.accept( session, inspector ) );
	}

	@Test
	public void testIsNull(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a " +
					"where a is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "Unknown Author" );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Test
	public void testIsNotNull(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a " +
					"where a is not null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "The Shining", "The Colour Out of Space" );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Test
	public void testIsNullImplicit(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"where book.author is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "Unknown Author" );
			inspector.assertNumberOfJoins( 0, 0 );
		} );
	}

	@Test
	public void testIsNullImplicitOrId(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"where book.author is null or book.author.id = 2",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "Unknown Author", "The Shining" );
			inspector.assertNumberOfJoins( 0, 0 );
		} );
	}

	@Test
	public void testIsNullImplicitJoinOrId(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a " +
					"where book.author is null or book.author.id = 2",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "Unknown Author", "The Shining" );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Test
	public void testDereferenceIsNull(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a " +
					"where a.id is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "Unknown Author" );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Test
	public void testDereferenceIsNotNull(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a " +
					"where a.id is not null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "The Shining", "The Colour Out of Space" );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Test
	public void testFkIsNull(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a " +
					"where fk(a) is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "Unknown Author" );
			// Even though we explicitly left join book.author the lazy table group is never
			// initialized as fk(a) always uses the association's owner fk column expression
			inspector.assertNumberOfJoins( 0, 0 );
		} );
	}

	@Test
	public void testFkIsNullOrId(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a " +
					"where fk(a) is null or a.id = 2",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "Unknown Author", "The Shining" );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Test
	public void testFkImplicitIsNull(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"where fk(book.author) is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "Unknown Author" );
			inspector.assertNumberOfJoins( 0, 0 );
		} );
	}

	@Test
	public void testFkImplicitIsNullOrId(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"where fk(book.author) is null or book.author.id = 2",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "Unknown Author", "The Shining" );
			inspector.assertNumberOfJoins( 0, 0 );
		} );
	}

	@Test
	public void testFkImplicitIsNullJoinOrId(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a " +
					"where fk(book.author) is null or book.author.id = 2",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "Unknown Author", "The Shining" );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Test
	public void testIsNotNullWithCondition(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a with a.alive = true " +
					"where a is not null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "The Shining" );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Test
	public void testIsNullWithCondition(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a with a.alive = true " +
					"where a is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "Unknown Author",
					"The Colour Out of Space" );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Test
	public void testDereferenceIsNotWithCondition(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a with a.alive = true " +
					"where a.id is not null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "The Shining" );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Test
	public void testDereferenceIsNullWithCondition(SessionFactoryScope scope) {
		withInspector( scope, (session, inspector) -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a with a.alive = true " +
					"where a.id is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "Unknown Author",
					"The Colour Out of Space" );
			inspector.assertNumberOfJoins( 0, 1 );
		} );
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		@GeneratedValue
		private Long id;

		private String title;

		@ManyToOne
		private Author author;

		public Book() {
		}

		public Book(String title, Author author) {
			this.title = title;
			this.author = author;
		}

		public String getTitle() {
			return title;
		}

		public Author getAuthor() {
			return author;
		}
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		private Long id;

		private String name;

		private boolean alive;

		public Author() {
		}

		public Author(Long id, String name, boolean alive) {
			this.id = id;
			this.name = name;
			this.alive = alive;
		}

		public String getName() {
			return name;
		}

		public boolean isAlive() {
			return alive;
		}
	}
}
