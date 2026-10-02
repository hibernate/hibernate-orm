package org.hibernate.orm.test.query.hql;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests fetching an association of a selected collection join while filtering its element collection.
///
/// @author Steve Ebersole
@JiraKey( "HHH-13990" )
@DomainModel( annotatedClasses = {
		ToOneFetchWithElementCollectionMemberOfTest.Author.class,
		ToOneFetchWithElementCollectionMemberOfTest.Book.class,
		ToOneFetchWithElementCollectionMemberOfTest.Publisher.class
} )
@SessionFactory
public class ToOneFetchWithElementCollectionMemberOfTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Publisher publisher = new Publisher( 1L, "Publisher" );
			session.persist( publisher );

			final Book publishedWinner = new Book( 1L, publisher, "Award" );
			final Book unpublishedWinner = new Book( 2L, null, "Award" );
			final Book otherAward = new Book( 3L, publisher, "Other award" );
			final Book otherAuthorWinner = new Book( 4L, publisher, "Award" );
			session.persist( publishedWinner );
			session.persist( unpublishedWinner );
			session.persist( otherAward );
			session.persist( otherAuthorWinner );

			final Author author = new Author( 1L );
			author.books.addAll( List.of( publishedWinner, unpublishedWinner, otherAward ) );
			session.persist( author );
			final Author otherAuthor = new Author( 2L );
			otherAuthor.books.add( otherAuthorWinner );
			session.persist( otherAuthor );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testFetchPublisherOfJoinedBookWithAwardFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> books = session.createQuery(
					"""
					select b from Author a
					join a.books b
					left join fetch b.publisher
					where :award member of b.awards and a = :author
					order by b.id
					""",
					Book.class
			)
					.setParameter( "award", "Award" )
					.setParameter( "author", session.getReference( Author.class, 1L ) )
					.getResultList();

			assertEquals( List.of( 1L, 2L ), books.stream().map( book -> book.id ).toList() );
			final Publisher publisher = books.get( 0 ).publisher;
			assertTrue( Hibernate.isInitialized( publisher ) );
			assertEquals( 1L, publisher.id );
			assertEquals( "Publisher", publisher.name );
			assertNull( books.get( 1 ).publisher );
		} );
	}

	@Entity( name = "Author" )
	@Table( name = "hhh13990_author" )
	public static class Author {
		@Id
		private Long id;

		@ManyToMany
		private List<Book> books = new ArrayList<>();

		public Author() {
		}

		public Author(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "Book" )
	@Table( name = "hhh13990_book" )
	public static class Book {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		private Publisher publisher;

		@ElementCollection
		private List<String> awards = new ArrayList<>();

		public Book() {
		}

		public Book(Long id, Publisher publisher, String award) {
			this.id = id;
			this.publisher = publisher;
			awards.add( award );
		}
	}

	@Entity( name = "Publisher" )
	@Table( name = "hhh13990_publisher" )
	public static class Publisher {
		@Id
		private Long id;

		private String name;

		public Publisher() {
		}

		public Publisher(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
