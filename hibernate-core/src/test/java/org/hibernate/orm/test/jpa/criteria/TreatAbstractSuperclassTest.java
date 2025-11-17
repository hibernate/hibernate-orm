/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		TreatAbstractSuperclassTest.LongBook.class,
		TreatAbstractSuperclassTest.ShortBook.class,
		TreatAbstractSuperclassTest.Article.class,
		TreatAbstractSuperclassTest.Author.class,
		TreatAbstractSuperclassTest.AuthorParticipation.class,
})
public class TreatAbstractSuperclassTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author author = new Author( "Frank Herbert" );
			final ShortBook book = new ShortBook( "Dune" );
			final AuthorParticipation participation = new AuthorParticipation( author, book );
			book.getParticipations().add( participation );
			session.persist( author );
			session.persist( book );
			session.persist( participation );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from AuthorParticipation" ).executeUpdate();
			session.createMutationQuery( "delete from Author" ).executeUpdate();
			session.createMutationQuery( "delete from Publication" ).executeUpdate();
		} );
	}

	@Test
	public void testJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<String> criteria = cb.createQuery( String.class );
			final Root<Publication> publicationRoot = criteria.from( Publication.class );
			// Treat as nested abstract superclass
			final Root<Book> bookRoot = cb.treat( publicationRoot, Book.class );
			final Join<Book, AuthorParticipation> join = bookRoot.join( "participations" );
			criteria.select( bookRoot.get( "title" ) ).where( cb.equal(
					join.get( "author" ).get( "name" ),
					"Frank Herbert"
			) );
			assertEquals( "Dune", session.createQuery( criteria ).getSingleResult() );
		} );
	}

	@Test
	public void testTreatMultiple(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Tuple> criteria = cb.createTupleQuery();
			final Root<Publication> publicationRoot = criteria.from( Publication.class );
			// Treat as nested abstract superclass
			final Root<Book> bookRoot = cb.treat( publicationRoot, Book.class );
			final Root<Article> articleRoot = cb.treat( publicationRoot, Article.class );
			criteria.multiselect(
					bookRoot.get( "title" ),
					articleRoot.get( "reference" )
			);
			final Tuple tuple = session.createQuery( criteria ).getSingleResult();
			assertEquals( "Dune", tuple.get( 0 ) );
			assertNull( tuple.get( 1 ) );
		} );
	}

	@Test
	public void testSubclassJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<String> criteria = cb.createQuery( String.class );
			final Root<Publication> publicationRoot = criteria.from( Publication.class );
			// Treat as child of nested abstract superclass
			final Root<ShortBook> shortBookRoot = cb.treat( publicationRoot, ShortBook.class );
			final Join<ShortBook, AuthorParticipation> join = shortBookRoot.join( "participations" );
			criteria.select( shortBookRoot.get( "title" ) ).where( cb.equal(
					join.get( "author" ).get( "name" ),
					"Frank Herbert"
			) );
			assertEquals( "Dune", session.createQuery( criteria ).getSingleResult() );
		} );
	}

	@MappedSuperclass
	public static class BaseEntity {
		@Id
		@GeneratedValue
		private long id;
	}

	@Entity(name = "Publication")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public abstract static class Publication extends BaseEntity {
		private String title;

		public Publication() {
		}

		public Publication(String title) {
			this.title = title;
		}
	}

	@Entity(name = "Article")
	public static class Article extends Publication {
		private String reference;

		public Article() {
		}

		public Article(String title) {
			super( title );
		}

		public String getReference() {
			return reference;
		}

		public void setReference(String reference) {
			this.reference = reference;
		}
	}

	@Entity(name = "Book")
	public static abstract class Book extends Publication {
		private String isbn;
		@OneToMany(mappedBy = "book", cascade = CascadeType.REMOVE)
		private List<AuthorParticipation> participations = new ArrayList<>();

		public Book() {
		}

		public Book(String title) {
			super( title );
		}

		public List<AuthorParticipation> getParticipations() {
			return participations;
		}
	}

	@Entity(name = "LongBook")
	public static class LongBook extends Book {
		private int pageCount;
		public LongBook() {
		}

		public LongBook(String title) {
			super( title );
		}
	}

	@Entity(name = "ShortBook")
	public static class ShortBook extends Book {
		private int readTime;
		public ShortBook() {
		}

		public ShortBook(String title) {
			super( title );
		}
	}

	@Entity(name = "Author")
	public static class Author extends BaseEntity {
		private String name;

		public Author() {
		}

		public Author(String name) {
			this.name = name;
		}
	}

	@Entity(name = "AuthorParticipation")
	public static class AuthorParticipation extends BaseEntity {
		@ManyToOne
		@JoinColumn(name = "author_id", nullable = false)
		private Author author;

		@ManyToOne
		@JoinColumn(name = "book_id", nullable = false)
		private Book book;

		public AuthorParticipation() {
		}

		public AuthorParticipation(Author author, Book book) {
			this.author = author;
			this.book = book;
		}
	}
}
