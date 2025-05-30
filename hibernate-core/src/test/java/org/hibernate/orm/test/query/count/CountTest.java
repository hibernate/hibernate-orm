/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.count;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {CountTest.Book.class, CountTest.Author.class, CountTest.Publisher.class})
public class CountTest {

	@Test void testCount(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.persist(new Book("9781932394153", "Hibernate in Action"));
			session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
		});
		scope.inSession(session -> {
			assertEquals(1L,
					session.createSelectionQuery("from Book where title like 'Hibernate%'", Book.class)
							.getResultCount());
			assertEquals(2L,
					session.createSelectionQuery("from Book where title like '%Hibernate%'", Book.class)
							.getResultCount());
			assertEquals(1L,
					session.createSelectionQuery("select isbn, title from Book where title like 'Hibernate%'", String.class)
							.getResultCount());
			assertEquals(1L,
					session.createSelectionQuery("from Book where title like :title", Book.class)
							.setParameter("title", "Hibernate%")
							.getResultCount());
			assertEquals(0L,
					session.createSelectionQuery("from Book where title like :title", Book.class)
							.setParameter("title", "Jibernate%")
							.getResultCount());
			assertEquals(2L,
					session.createSelectionQuery("select title from Book where title like '%Hibernate' union select title from Book where title like 'Hibernate%'", String.class)
							.getResultCount());
			assertEquals(2L,
					session.createSelectionQuery("from Book left join fetch authors left join fetch publisher", Book.class)
							.getResultCount());
			assertEquals(0L,
					session.createSelectionQuery("from Book join fetch publisher", Book.class)
							.getResultCount());
		});
	}

	@Test void testCountNative(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.persist(new Book("9781932394153", "Hibernate in Action"));
			session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
		});
		scope.inSession(session -> {
			assertEquals(2L,
					session.createNativeQuery("select title from books", String.class)
							.setMaxResults(1)
							.getResultCount());
			assertEquals(1L,
					session.createNativeQuery("select title from books where title like :title", String.class)
							.setParameter("title", "Hibernate%")
							.getResultCount());
			assertEquals(2L,
					session.createNativeQuery("select title from books", String.class)
							.setMaxResults(1)
							.getResultCount());
		});
	}

	@Test void testCountCriteria(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.persist(new Book("9781932394153", "Hibernate in Action"));
			session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
		});
		CriteriaBuilder builder = scope.getSessionFactory().getCriteriaBuilder();
		scope.inSession(session -> {
			CriteriaQuery<Book> query1 = builder.createQuery(Book.class);
			query1.where( builder.like( query1.from(Book.class).get("title"), "Hibernate%" ) );
			assertEquals(1L,
					session.createQuery(query1)
							.getResultCount());
			CriteriaQuery<Book> query2 = builder.createQuery(Book.class);
			query2.from(Book.class);
			assertEquals(2L,
					session.createQuery(query2)
							.setMaxResults(1)
							.getResultCount());
			CriteriaQuery<Book> query3 = builder.createQuery(Book.class);
			ParameterExpression<String> parameter = builder.parameter(String.class);
			query3.where( builder.like( query3.from(Book.class).get("title"), parameter ) );
			assertEquals(1L,
					session.createQuery(query3)
							.setParameter(parameter, "Hibernate%")
							.getResultCount());
			CriteriaQuery<Book> query4 = builder.createQuery(Book.class);
			Root<Book> book = query4.from(Book.class);
			book.fetch("authors", JoinType.INNER);
			assertEquals(0L,
					session.createQuery(query4)
							.getResultCount());
		});
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-19065" )
	public void testJoins(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Publisher p = new Publisher( 1L, "Manning" );
			session.persist( p );
			final Book book = new Book( "9781932394153", "Hibernate in Action" );
			book.publisher = p;
			session.persist( book );
			session.persist( new Book( "9781617290459", "Java Persistence with Hibernate" ) );
		} );
		scope.inSession( session -> {
			// explicit inner join
			assertCount( 1, "select p from Book b join b.publisher p", Publisher.class, session );
			assertCount( 1, "select p.name from Book b join b.publisher p", String.class, session );
			// explicit left join
			assertCount( 2, "select p from Book b left join b.publisher p", Publisher.class, session );
			assertCount( 2, "select p.name from Book b left join b.publisher p", String.class, session );
			// implicit join
			assertCount( 1, "select b.publisher from Book b", Publisher.class, session );
			assertCount( 1, "select b.publisher from Book b join b.publisher", Publisher.class, session );
			assertCount( 1, "select b.publisher.name from Book b", String.class, session );
			assertCount( 1, "select publisher.name from Book b left join b.publisher", String.class, session );
			assertCount( 1,
					"select publisher.name from Book b left join b.publisher where publisher.name is null or length(publisher.name) > 0",
					String.class, session );
			// selecting only the id does not create an explicit join
			assertCount( 2, "select b.publisher.id from Book b", Long.class, session );
		} );
	}

	private <T> void assertCount(int expected, String hql, Class<T> resultClass, SessionImplementor session) {
		final QueryImplementor<T> query = session.createQuery( hql, resultClass );
		final List<T> resultList = query.getResultList();
		final long resultCount = query.getResultCount();
		assertEquals( expected, resultList.size() );
		assertEquals( expected, resultCount );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name="Book")
	@Table(name = "books")
	static class Book {
		@Id String isbn;
		String title;

		@ManyToMany
		List<Author> authors;

		@ManyToOne
		Publisher publisher;

		Book(String isbn, String title) {
			this.isbn = isbn;
			this.title = title;
		}

		Book() {
		}
	}

	@Entity(name="Author")
	@Table(name = "authors")
	static class Author {
		@Id String ssn;
		String name;
	}

	@Entity(name="Publisher")
	@Table(name = "pubs")
	static class Publisher {
		@Id Long id;
		String name;

		Publisher() {
		}

		Publisher(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
