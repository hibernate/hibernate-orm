/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.restriction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.query.Order;
import org.hibernate.query.Restriction;
import org.hibernate.query.range.Range;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = RestrictionTest.Book.class)
public class RestrictionTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery("delete Book").executeUpdate() );
		scope.inTransaction( session -> {
			session.persist(new Book("9781932394153", "Hibernate in Action", 400));
			session.persist(new Book("9781617290459", "Java Persistence with Hibernate", 1000));
		});

		var bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		@SuppressWarnings( "unchecked" )
		var title = (SingularAttribute<? super Book, String>) bookType.findSingularAttribute("title");
		@SuppressWarnings( "unchecked" )
		var isbn = (SingularAttribute<? super Book, String>) bookType.findSingularAttribute("isbn");
		@SuppressWarnings( "unchecked" )
		var pages = (SingularAttribute<? super Book, Integer>) bookType.findSingularAttribute("pages");

		Book book = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class )
						.addRestriction( Restriction.equal( isbn, "9781932394153" ) )
						.getSingleResult() );
		assertEquals( "Hibernate in Action", book.title );
		List<Book> books = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class)
						.addRestriction( Restriction.like( title, "%Hibernate%" ) )
						.setOrder( Order.desc( title ) )
						.getResultList() );
		assertEquals( 2, books.size() );
		assertEquals( "Java Persistence with Hibernate", books.get(0).title );
		assertEquals( "Hibernate in Action", books.get(1).title );
		List<Book> booksByIsbn = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class)
						.addRestriction( Restriction.in( isbn, List.of("9781932394153", "9781617290459") ) )
						.setOrder( Order.asc( title ) )
						.getResultList() );
		assertEquals( 2, booksByIsbn.size() );
		assertEquals( "Hibernate in Action", booksByIsbn.get(0).title );
		assertEquals( "Java Persistence with Hibernate", booksByIsbn.get(1).title );
		List<Book> booksByPages = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class)
						.addRestriction( Restriction.greaterThan( pages, 500 ) )
						.getResultList() );
		assertEquals( 1, booksByPages.size() );
		List<Book> booksByPageRange = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class)
						.addRestriction( Restriction.between( pages, 150, 400 ) )
						.getResultList() );
		assertEquals( 1, booksByPageRange.size() );
		Book bookByTitle = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class)
						.addRestriction( Restriction.equalIgnoringCase( title, "hibernate in action" ) )
						.getSingleResultOrNull() );
		assertEquals( "9781932394153", bookByTitle.isbn );
		Book bookByTitleUnsafe = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class)
						.addRestriction( Restriction.restrict( Book.class, "title",
								Range.singleCaseInsensitiveValue("hibernate in action") ) )
						.getSingleResultOrNull() );
		assertEquals( "9781932394153", bookByTitleUnsafe.isbn );
		List<Book> allBooks = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class)
						.addRestriction( Restriction.unrestricted() )
						.getResultList() );
		assertEquals( 2, allBooks.size() );
		List<Book> noBooks = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class)
						.addRestriction( Restriction.unrestricted().negated() )
						.getResultList() );
		assertEquals( 0, noBooks.size() );
		List<Book> books1 = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class)
						.addRestriction( Restriction.endWith(title, "Hibernate") )
						.getResultList() );
		assertEquals( 1, books1.size() );
		List<Book> books2 = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class)
						.addRestriction( Restriction.like(title, "*Hibernat?", false, '?', '*') )
						.getResultList() );
		assertEquals( 1, books2.size() );
		List<Book> books3 = scope.fromSession( session ->
				session.createSelectionQuery( "from Book", Book.class)
						.addRestriction( Restriction.contains(title, "Hibernate") )
						.getResultList() );
		assertEquals( 2, books3.size() );
	}

	@Entity(name="Book")
	static class Book {
		@Id
		String isbn;
		String title;
		int pages;

		Book(String isbn, String title, int pages) {
			this.isbn = isbn;
			this.title = title;
			this.pages = pages;
		}

		Book() {
		}
	}

}
