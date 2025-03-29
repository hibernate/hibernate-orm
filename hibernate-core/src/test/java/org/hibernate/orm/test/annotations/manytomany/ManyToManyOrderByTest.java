/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				ManyToManyOrderByTest.Author.class,
				ManyToManyOrderByTest.Book.class,
		}
)
@SessionFactory
@JiraKey(value = "HHH-16165")
public class ManyToManyOrderByTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Author author = new Author( "Iain M Banks" );
					Book book1 = new Book( "Feersum Endjinn" );
					Book book2 = new Book( "Use of Weapons" );
					author.books.add( book1 );
					author.books.add( book2 );
					session.persist( book1 );
					session.persist( book2 );
					session.persist( author );
				}
		);
	}

	@Test
	public void testQueryWithDistinct(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Author author = session.createQuery(
									"select distinct a from Author a left join fetch a.books",
									Author.class
							)
							.getSingleResult();
					assertThat( author ).isNotNull();
				}
		);
	}

	@Test
	public void testQueryWithDistinct2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Tuple> results = session.createQuery(
									"select distinct a, 1 from Author a left join fetch a.books",
									Tuple.class
							)
							.list();
					for ( Tuple t : results ) {
						assertThat( t.getElements().size() ).isEqualTo( 2 );
						assertThat( t.get( 0 ) ).isInstanceOf( Author.class );
						assertThat( t.get( 1 ) ).isEqualTo( 1 );
					}
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Author author = session.createQuery(
									"select a from Author a left join fetch a.books",
									Author.class
							)
							.getSingleResult();
					assertThat( author ).isNotNull();
				}
		);
	}

	@Entity(name = "Book")
	@Table(name = "MTMBook")
	static class Book {

		@GeneratedValue
		@Id
		long id;

		@Basic(optional = false)
		String title;

		Book(String title) {
			this.title = title;
		}

		Book() {
		}
	}

	@Entity(name = "Author")
	@Table(name = "MTMAuthor")
	static class Author {

		@GeneratedValue
		@Id
		long id;

		@Basic(optional = false)
		String name;

		@ManyToMany
		@OrderBy("id")
		List<Book> books = new ArrayList<>();

		Author(String name) {
			this.name = name;
		}

		public Author() {
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Book> getBooks() {
			return books;
		}
	}
}
