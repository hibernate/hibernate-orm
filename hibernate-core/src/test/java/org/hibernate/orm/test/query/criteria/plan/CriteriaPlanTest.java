/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.plan;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {CriteriaPlanTest.Author.class, CriteriaPlanTest.Book.class})
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.CRITERIA_COPY_TREE, value = "false"),
				@Setting(name = AvailableSettings.CRITERIA_PLAN_CACHE_ENABLED, value = "true"),
		}
)
@SessionFactory
class CriteriaPlanTest {

	@Test
	void criteriaPlanCacheWithEntityParameters(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author author = populateData(session);

			assertThat(runQuery(session, author)).hasSize(5);
			assertThat(runQuery(session, author)).hasSize(5);
		} );
	}

	private static List<Book> runQuery(SessionImplementor session, Author author) {
		final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
		final JpaCriteriaQuery<Book> q = cb.createQuery(Book.class);
		final JpaRoot<Book> root = q.from(Book.class);
		q.select(root);
		q.where(cb.equal(root.get("author"), author));
		return session.createQuery(q).getResultList();
	}

	public Author populateData(SessionImplementor entityManager) {
		final Author author = new Author();
		author.name = "David Gourley";
		entityManager.persist(author);

		for (int i = 0; i < 5; i++) {
			final Book book = new Book();
			book.name = "HTTP Definitive guide " + i;
			book.author = author;
			entityManager.persist(book);
			author.books.add(book);
		}

		return author;
	}

	@Entity(name = "Author")
	@Table(name = "Author")
	public static class Author {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long authorId;

		@Column
		public String name;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
		public List<Book> books = new ArrayList<>();

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			final Author author = (Author) o;
			return authorId.equals(author.authorId);
		}

		@Override
		public int hashCode() {
			return authorId.hashCode();
		}
	}

	@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
	@Entity(name = "Book")
	@Table(name = "Book")
	public static class Book {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long bookId;

		@Column
		public String name;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "author_id", nullable = false)
		public Author author;
	}
}
