/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Jpa(
		annotatedClasses = { ReloadWithPreviousRowEntityTest.Book.class, ReloadWithPreviousRowEntityTest.Author.class, ReloadWithPreviousRowEntityTest.AuthorDetails.class }
)
@Jira("https://hibernate.atlassian.net/browse/HHH-18271")
public class ReloadWithPreviousRowEntityTest {

	@BeforeEach
	public void prepareTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Book book1 = new Book();
			book1.name = "Book 1";
			final Book book2 = new Book();
			book2.name = "Book 2";
			final Book book3 = new Book();
			book3.name = "Book 3";

			final Author author1 = new Author();
			author1.name = "Author 1";
			final Author author2 = new Author();
			author2.name = "Author 2";

			final AuthorDetails details1 = new AuthorDetails();
			details1.name = "Author Details";
			details1.author = author1;
			author1.details = details1;

			final AuthorDetails details2 = new AuthorDetails();
			details2.name = "Author Details";
			details2.author = author2;
			author2.details = details2;

			author1.books.add( book1 );
			author1.books.add( book2 );
			author1.books.add( book3 );
			book1.author = author1;
			book2.author = author1;
			book3.author = author2;
			details1.favoriteBook = book3;

			entityManager.persist( author1 );
			entityManager.persist( author2 );
			entityManager.persist( book1 );
			entityManager.persist( book2 );
			entityManager.persist( book3 );
		} );
	}

	@Test
	public void testReloadWithPreviousRow(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// Load authors into persistence context
			Author author = em.createQuery( "from Author a join fetch a.details where a.name = 'Author 1'", Author.class ).getSingleResult();
			em.createQuery( "from Author a join fetch a.details d left join fetch d.favoriteBook join fetch a.books where a.name = 'Author 1'", Author.class ).getResultList();
			Assertions.assertTrue( Hibernate.isInitialized( author.details.favoriteBook ) );
			Assertions.assertTrue( Hibernate.isInitialized( author.books ) );
		} );
	}

	@Entity(name = "Author")
	@Table(name = "Author")
	public static class Author {
		@Id
		@GeneratedValue
		public Long authorId;

		@Column
		public String name;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
		public List<Book> books = new ArrayList<>();

		@OneToOne(optional = false, fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
		public AuthorDetails details;

	}

	@Entity(name = "AuthorDetails")
	@Table(name = "AuthorDetails")
	public static class AuthorDetails {
		@Id
		@GeneratedValue
		public Long detailsId;

		@Column
		public String name;

		@OneToOne(fetch = FetchType.LAZY, mappedBy = "details", optional = false)
		public Author author;

		@ManyToOne(fetch = FetchType.LAZY)
		public Book favoriteBook;

		public String getName() {
			return name;
		}
	}

	@Entity(name = "Book")
	@Table(name = "Book")
	public static class Book {
		@Id
		@GeneratedValue
		public Long bookId;

		@Column
		public String name;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "author_id", nullable = false)
		public Author author;
	}

}
