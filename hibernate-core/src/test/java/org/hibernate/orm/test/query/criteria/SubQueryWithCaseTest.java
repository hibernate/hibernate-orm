/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaDerivedRoot;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel (
		annotatedClasses = {SubQueryWithCaseTest.Book.class, SubQueryWithCaseTest.Isbn.class}
)
@SessionFactory
public class SubQueryWithCaseTest {

	@BeforeAll
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Isbn isbn1 = new Isbn("123");
					Isbn isbn2 = new Isbn("none");
					Book book1 = new Book(1, isbn1);
					Book book2 = new Book(2, isbn2);
					session.persist( book1 );
					session.persist( book2 );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.getSessionFactory().getSchemaManager().truncate()
		);
	}

	@Test
	public void testCriteriaSubqueryWithCase(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					var cb = session.getCriteriaBuilder();
					JpaCriteriaQuery<Tuple> query = cb.createTupleQuery();
					JpaSubQuery<Tuple> subquery = query.subquery(Tuple.class);
					JpaRoot<Book> sqRoot = subquery.from(Book.class);
					JpaJoin<Book, Isbn> isbnJoin = sqRoot.join( SubQueryWithCaseTest_.Book_.isbn);
					JpaSimpleCase<String, String> isbnExpr = cb.<String, String>selectCase(isbnJoin.get(SubQueryWithCaseTest_.Isbn_.isbn))
							.when("none", cb.nullLiteral(String.class))
							.otherwise(isbnJoin.get(SubQueryWithCaseTest_.Isbn_.isbn));
					subquery.multiselect(
							sqRoot.get(SubQueryWithCaseTest_.Book_.id).alias("id"),
							// Also add a selection item with an alias, that matches the also selected isbn path
							// to provoke a potential alias collision
							sqRoot.get(SubQueryWithCaseTest_.Book_.title).alias("isbn"),
							isbnExpr.alias("isbn1")
					);
					JpaDerivedRoot<Tuple> root = query.from(subquery);
					query.select(cb.tuple(
							root.get("id").alias("id"),
							root.get("isbn1").alias("isbn")
					)).orderBy(
							cb.asc(root.get("id"))
					);
					List<Tuple> list = session.createQuery(query).getResultList();
					assertThat(list).hasSize(2);
					Iterator<Tuple> it = list.iterator();
					Tuple tuple = it.next();
					assertThat(tuple.get("id")).isEqualTo(1);
					assertThat(tuple.get("isbn")).isEqualTo("123");
					tuple = it.next();
					assertThat(tuple.get("id")).isEqualTo(2);
					assertThat(tuple.get("isbn")).isEqualTo(null);

				}
		);
	}

	@Entity(name = "Book")
	@Table(name = "t_book")
	public static class Book {
		@Id
		private int id;
		private String title;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@JoinColumn(name = "isbn")
		private Isbn isbn;

		public Book() {
		}

		public Book(int id, Isbn isbn) {
			this.id = id;
			this.isbn = isbn;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Isbn getIsbn() {
			return isbn;
		}

		public void setIsbn(Isbn isbn) {
			this.isbn = isbn;
		}
	}

	@Entity(name = "Isbn")
	@Table(name = "t_isbn")
	public static class Isbn {
		@Id
		private String isbn;

		public Isbn() {
		}

		public Isbn(String isbn) {
			this.isbn = isbn;
		}

		public String getIsbn() {
			return isbn;
		}

		public void setIsbn(String isbn) {
			this.isbn = isbn;
		}
	}

}
