/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.processor.test.integ.dao._BookDao;
import org.hibernate.processor.test.integ.model.Book;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(annotatedClasses = { Book.class })
@SessionFactory
class BookDaoTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testFindById(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-1", "Effective Java", "Joshua Bloch", 416 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			Book found = dao.getBook( "isbn-1" );
			assertNotNull( found );
			assertEquals( "Effective Java", found.getTitle() );
			assertEquals( 416, found.getPages() );
		} );
	}

	@Test
	void testFindByIdNotFound(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			assertThrows( ObjectNotFoundException.class, () -> dao.getBook( "nonexistent" ) );
		} );
	}

	@Test
	void testFindByTitle(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-1", "Java", "Author A", 100 ) );
			session.insert( new Book( "isbn-2", "Java", "Author B", 200 ) );
			session.insert( new Book( "isbn-3", "Python", "Author C", 300 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			List<Book> books = dao.getBooksByTitle( "Java" );
			assertEquals( 2, books.size() );
		} );
	}

	@Test
	void testFindByTitleAndAuthor(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-1", "Java", "Author A", 100 ) );
			session.insert( new Book( "isbn-2", "Java", "Author B", 200 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			Book book = dao.getBookByTitleAndAuthor( "Java", "Author B" );
			assertEquals( "isbn-2", book.getIsbn() );
		} );
	}

	@Test
	void testHqlFindByTitle(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-1", "Java Programming", "Author A", 300 ) );
			session.insert( new Book( "isbn-2", "Java Concurrency", "Author B", 400 ) );
			session.insert( new Book( "isbn-3", "Python Basics", "Author C", 200 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			List<Book> javaBooks = dao.findBooksByTitle( "Java%" );
			assertEquals( 2, javaBooks.size() );
		} );
	}

	@Test
	void testHqlFindWithMinPages(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-1", "Short", "A", 50 ) );
			session.insert( new Book( "isbn-2", "Medium", "B", 200 ) );
			session.insert( new Book( "isbn-3", "Long", "C", 500 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			List<Book> longBooks = dao.findBooksWithMorePages( 100 );
			assertEquals( 2, longBooks.size() );
			assertEquals( "Long", longBooks.get( 0 ).getTitle() );
		} );
	}

	@Test
	void testHqlCount(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			assertEquals( 0, dao.countBooks() );
		} );
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-1", "One", "A", 100 ) );
			session.insert( new Book( "isbn-2", "Two", "B", 200 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			assertEquals( 2, dao.countBooks() );
		} );
	}

	@Test
	void testHqlFindByIsbn(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-find", "Title", "Author", 100 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			Book found = dao.findByIsbn( "isbn-find" );
			assertEquals( "Title", found.getTitle() );
		} );
	}

	@Test
	void testHqlDelete(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-del", "To Delete", "Author", 100 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			assertEquals( 1, dao.countBooks() );
			int deleted = dao.deleteByIsbn( "isbn-del" );
			assertEquals( 1, deleted );
			assertEquals( 0, dao.countBooks() );
		} );
	}

	@Test
	void testHqlFindByIsbnNullable(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-kotlin", "Kotlin Programming", "Author K", 100 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			Book found = dao.findByIsbnNullable( "isbn-kotlin" );
			assertNotNull( found );
			assertEquals( "Kotlin Programming", found.getTitle() );
			Book notFound = dao.findByIsbnNullable( "nonexistent" );
			assertNull( notFound );
		} );
	}

	@Test
	void testHqlFindByIsbnNullableJspecify(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-jspecify", "JSpecify Programming", "Author J", 100 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			Book found = dao.findByIsbnNullableJspecify( "isbn-jspecify" );
			assertNotNull( found );
			assertEquals( "JSpecify Programming", found.getTitle() );
			Book notFound = dao.findByIsbnNullableJspecify( "nonexistent" );
			assertNull( notFound );
		} );
	}

	@Test
	void testSqlFindByIsbn(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-native", "Native Title", "Author", 100 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			Book found = dao.findByIsbnNative( "isbn-native" );
			assertNotNull( found );
			assertEquals( "Native Title", found.getTitle() );
		} );
	}

	@Test
	void testSqlCount(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-nc1", "One", "A", 100 ) );
			session.insert( new Book( "isbn-nc2", "Two", "B", 200 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var dao = new _BookDao( session );
			assertEquals( 2, dao.countBooksNative() );
		} );
	}
}
