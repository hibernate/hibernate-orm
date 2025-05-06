/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.restriction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.query.Order.asc;
import static org.hibernate.query.Order.desc;
import static org.hibernate.query.range.Range.containing;
import static org.hibernate.query.range.Range.greaterThan;
import static org.hibernate.query.range.Range.singleCaseInsensitiveValue;
import static org.hibernate.query.restriction.Path.from;
import static org.hibernate.query.restriction.Restriction.all;
import static org.hibernate.query.restriction.Restriction.any;
import static org.hibernate.query.restriction.Restriction.between;
import static org.hibernate.query.restriction.Restriction.contains;
import static org.hibernate.query.restriction.Restriction.endsWith;
import static org.hibernate.query.restriction.Restriction.equal;
import static org.hibernate.query.restriction.Restriction.equalIgnoringCase;
import static org.hibernate.query.restriction.Restriction.greaterThan;
import static org.hibernate.query.restriction.Restriction.in;
import static org.hibernate.query.restriction.Restriction.like;
import static org.hibernate.query.restriction.Restriction.restrict;
import static org.hibernate.query.restriction.Restriction.unrestricted;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {RestrictionTest.Book.class, RestrictionTest.Publisher.class})
public class RestrictionTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
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
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( equal( isbn, "9781932394153" ) )
						.createQuery( session )
						.getSingleResult() );
		assertEquals( "Hibernate in Action", book.title );
		List<Book> books = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( like( title, "%Hibernate%" ) )
						.sort( desc( title ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 2, books.size() );
		assertEquals( "Java Persistence with Hibernate", books.get(0).title );
		assertEquals( "Hibernate in Action", books.get(1).title );
		List<Book> booksByIsbn = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( in( isbn, List.of("9781932394153", "9781617290459") ) )
						.sort( asc( title ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 2, booksByIsbn.size() );
		assertEquals( "Hibernate in Action", booksByIsbn.get(0).title );
		assertEquals( "Java Persistence with Hibernate", booksByIsbn.get(1).title );
		List<Book> booksByPages = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( greaterThan( pages, 500 ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 1, booksByPages.size() );
		List<Book> booksByPageRange = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( between( pages, 150, 400 ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 1, booksByPageRange.size() );
		Book bookByTitle = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( equalIgnoringCase( title, "hibernate in action" ) )
						.createQuery( session )
						.getSingleResultOrNull() );
		assertEquals( "9781932394153", bookByTitle.isbn );
		Book bookByTitleUnsafe = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( restrict( Book.class, "title",
								singleCaseInsensitiveValue("hibernate in action") ) )
						.createQuery( session )
						.getSingleResultOrNull() );
		assertEquals( "9781932394153", bookByTitleUnsafe.isbn );
		List<Book> allBooks = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( unrestricted() )
						.createQuery( session )
						.getResultList() );
		assertEquals( 2, allBooks.size() );
		List<Book> noBooks = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( unrestricted().negated() )
						.createQuery( session )
						.getResultList() );
		assertEquals( 0, noBooks.size() );
		List<Book> books1 = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( endsWith(title, "Hibernate") )
						.createQuery( session )
						.getResultList() );
		assertEquals( 1, books1.size() );
		List<Book> books2 = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( like(title, "*Hibernat?", false, '?', '*') )
						.createQuery( session )
						.getResultList() );
		assertEquals( 1, books2.size() );
		List<Book> books3 = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( contains(title, "Hibernate") )
						.createQuery( session )
						.getResultList() );
		assertEquals( 2, books3.size() );
		List<Book> booksByTitleAndIsbn = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( all( contains(title, "Hibernate"),
								equal( isbn, "9781932394153" ) ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 1, booksByTitleAndIsbn.size() );
		List<Book> booksByTitleOrIsbn = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( any( contains(title, "Hibernate"),
								equal( isbn, "9781932394153" ) ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 2, booksByTitleOrIsbn.size() );
		List<Book> booksByIsbn1 = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( in( isbn, "9781932394153", "9781617290459", "XYZ" ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 2, booksByIsbn1.size() );
		List<Book> booksByIsbn2 = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( in( isbn, List.of("9781617290459", "XYZ", "ABC") ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 1, booksByIsbn2.size() );
	}

	@Test
	void testPath(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( session -> {
			Publisher pub = new Publisher();
			pub.name = "Manning";
			session.persist( pub );
			session.persist( new Book( "9781932394153", "Hibernate in Action", 400, pub ) );
			session.persist( new Book( "9781617290459", "Java Persistence with Hibernate", 1000, pub ) );
		} );

		var bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		var pubType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Publisher.class);
		@SuppressWarnings( "unchecked" )
		var title = (SingularAttribute<? super Book, String>) bookType.findSingularAttribute("title");
		@SuppressWarnings( "unchecked" )
		var isbn = (SingularAttribute<? super Book, String>) bookType.findSingularAttribute("isbn");
		@SuppressWarnings( "unchecked" )
		var pages = (SingularAttribute<? super Book, Integer>) bookType.findSingularAttribute("pages");
		@SuppressWarnings( "unchecked" )
		var publisher = (SingularAttribute<? super Book, Publisher>) bookType.findSingularAttribute("publisher");
		@SuppressWarnings( "unchecked" )
		var name = (SingularAttribute<? super Publisher, String>) pubType.findSingularAttribute("name");
		@SuppressWarnings( "unchecked" )
		var version = (SingularAttribute<? super Publisher, Integer>) pubType.findSingularAttribute("version");

		scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( from(Book.class).equalTo( session.find(Book.class, "9781932394153") ) )
						.createQuery( session )
						.getSingleResult() );

		List<Book> booksInIsbn = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( from(Book.class).to(isbn).in( List.of("9781932394153", "9781617290459") ) )
						.sort( desc( isbn ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 2, booksInIsbn.size() );
		List<Book> booksWithPub = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( from(Book.class).to(publisher).to(name).equalTo("Manning") )
						.sort( desc( title ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 2, booksWithPub.size() );
		List<Book> noBookWithPub = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( from(Book.class).to(publisher).to(name).notEqualTo("Manning") )
						.sort( desc( title ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 0, noBookWithPub.size() );
		List<Book> books = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( from(Book.class).to(title).restrict( containing("hibernate", false) ) )
						.sort( desc( title ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 2, books.size() );
		List<Book> booksWithPubVersion = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( from(Book.class).to(publisher).to(version).restrict( greaterThan(5) ) )
						.createQuery( session )
						.getResultList() );
		assertEquals( 0, booksWithPubVersion.size() );
		List<Book> unsafeTest = scope.fromSession( session ->
				SelectionSpecification.create( Book.class, "from Book" )
						.restrict( from(Book.class)
								.to("publisher", Publisher.class)
								.to("name", String.class).equalTo("Manning") )
						.createQuery( session )
						.getResultList() );
		assertEquals( 2, unsafeTest.size() );
	}

	@Test
	void testCriteria(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
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

		scope.inSession( session -> {
			var query = session.getCriteriaBuilder().createQuery(String.class);
			var root = query.from( Book.class );
			like( title, "%Hibernate%" ).apply( query, root );
			query.select( root.get( title ) );
			List<String> titles = session.createQuery( query ).getResultList();
			assertEquals( 2, titles.size() );
		} );
		scope.inSession( session -> {
			var query = session.getCriteriaBuilder().createQuery(String.class);
			var root = query.from( Book.class );
			equal( isbn, "9781932394153" ).apply( query, root );
			query.select( root.get( title ) );
			List<String> titles = session.createQuery( query ).getResultList();
			assertEquals( 1, titles.size() );
		} );
		scope.inSession( session -> {
			var query = session.getCriteriaBuilder().createQuery("select title from Book", String.class);
			var root = query.getRoot(0, Book.class);
			equal( isbn, "9781932394153" ).apply( query, root );
			List<String> titles = session.createQuery( query ).getResultList();
			assertEquals( 1, titles.size() );
		} );

		var builder = scope.getSessionFactory().getCriteriaBuilder();
		var query = builder.createQuery("from Book where pages > 200", Book.class);
		var root = query.getRoot(0, Book.class);
		like( title, "Hibernate%" ).apply( query, root );
		query.orderBy(builder.asc(root.get(title)), builder.desc(root.get(isbn)));
		scope.inSession( session -> {
			List<Book> matchingBooks = session.createSelectionQuery(query).getResultList();
			assertEquals( 1, matchingBooks.size() );
		});

	}


	@Entity(name="Book")
	static class Book {
		@Id
		String isbn;
		String title;
		int pages;

		@ManyToOne
		Publisher publisher;

		Book(String isbn, String title, int pages) {
			this.isbn = isbn;
			this.title = title;
			this.pages = pages;
		}

		Book(String isbn, String title, int pages, Publisher publisher) {
			this.isbn = isbn;
			this.title = title;
			this.pages = pages;
			this.publisher = publisher;
		}

		Book() {
		}
	}

	@Entity(name="Publisher")
	static class Publisher{
		@Id String name;
		@Version int version;
	}

}
