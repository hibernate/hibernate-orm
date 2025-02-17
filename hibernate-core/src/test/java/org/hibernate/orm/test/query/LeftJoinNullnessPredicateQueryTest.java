/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
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
			final Author hpLovecraft = new Author( "Howard Phillips Lovecraft", false );
			final Author sKing = new Author( "Stephen King", true );
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

	@Test
	public void testIsNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a " +
					"where a is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "Unknown Author" );
		} );
	}

	@Test
	public void testIsNotNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a " +
					"where a is not null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "The Shining", "The Colour Out of Space" );
		} );
	}

	@Test
	public void testIsNullImplicit(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"where book.author is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "Unknown Author" );
		} );
	}

	@Test
	public void testDereferenceIsNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
							"left join book.author a " +
							"where a.id is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "Unknown Author" );
		} );
	}

	@Test
	public void testDereferenceIsNotNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
							"left join book.author a " +
							"where a.id is not null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "The Shining", "The Colour Out of Space" );
		} );
	}

	@Test
	public void testFkImplicitIsNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"where fk(book.author) is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "Unknown Author" );
		} );
	}

	@Test
	public void testIsNotNullWithCondition(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a with a.alive = true " +
					"where a is not null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "The Shining" );
		} );
	}

	@Test
	public void testIsNullWithCondition(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
					"left join book.author a with a.alive = true " +
					"where a is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "Unknown Author", "The Colour Out of Space" );
		} );
	}

	@Test
	public void testDereferenceIsNotWithCondition(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
							"left join book.author a with a.alive = true " +
							"where a.id is not null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getTitle() ).isEqualTo( "The Shining" );
		} );
	}

	@Test
	public void testDereferenceIsNullWithCondition(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> resultList = session.createQuery(
					"select book from Book book " +
							"left join book.author a with a.alive = true " +
							"where a.id is null",
					Book.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( b -> b.title ) ).contains( "Unknown Author", "The Colour Out of Space" );
		} );
	}

	@Entity( name = "Book" )
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

	@Entity( name = "Author" )
	public static class Author {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private boolean alive;

		public Author() {
		}

		public Author(String name, boolean alive) {
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
