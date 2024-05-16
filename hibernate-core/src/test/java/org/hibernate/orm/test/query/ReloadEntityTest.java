/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
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

		@OneToOne(fetch = FetchType.EAGER, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
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
