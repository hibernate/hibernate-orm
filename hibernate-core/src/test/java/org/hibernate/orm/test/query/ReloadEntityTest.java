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
		annotatedClasses = { ReloadEntityTest.Book.class, ReloadEntityTest.Author.class, ReloadEntityTest.AuthorDetails.class }
)
@Jira("https://hibernate.atlassian.net/browse/HHH-18271")
public class ReloadEntityTest {

	@BeforeEach
	public void prepareTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Book book = new Book();
			book.name = "HTTP Definitive guide";

			final Author author = new Author();
			author.name = "David Gourley";

			final AuthorDetails details = new AuthorDetails();
			details.name = "Author Details";
			details.author = author;
			author.details = details;

			author.books.add( book );
			book.author = author;

			entityManager.persist( author );
			entityManager.persist( book );
		} );
	}

	@Test
	public void testReload(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final List<Author> authors1 = em.createQuery( "from Author", Author.class ).getResultList();
			final List<Author> authors2 = em.createQuery( "from Author", Author.class ).getResultList();
		} );
	}

	@Test
	public void testFlushAndReload(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Load an Author with EAGER details
			final List<Author> authors1 = em.createQuery( "from Author", Author.class ).getResultList();
			final Author author = authors1.get(0);

			// Create a new details object and then detach it
			final AuthorDetails details = new AuthorDetails();
			details.name = "Author Details";
			details.author = author;
			author.details = null;
			em.persist( details );
			em.flush();
			em.detach( details );

			// Replace the details with a lazy proxy
			author.details = em.getReference( AuthorDetails.class, details.detailsId );
			em.flush();

			Assertions.assertFalse( Hibernate.isInitialized( author.details ) );

			final List<Author> authors2 = em.createQuery( "from Author join fetch details", Author.class ).getResultList();
			final Author author2 = authors2.get(0);
			Assertions.assertTrue( Hibernate.isInitialized( author2.details ) );
			Assertions.assertEquals( details.name, author2.details.getName() );
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

		@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
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
