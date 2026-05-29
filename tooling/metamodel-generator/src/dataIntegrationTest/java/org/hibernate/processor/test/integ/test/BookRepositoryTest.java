/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import jakarta.data.exceptions.DataException;
import org.hibernate.processor.test.integ.model.Book;
import org.hibernate.processor.test.integ.repository.BookRepository;
import org.hibernate.processor.test.integ.repository._BookRepository;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = { Book.class, BookRepository.class })
@SessionFactory
class BookRepositoryTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testInsertAndFind(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			repo.insert( new Book( "978-0-13-468599-1", "Effective Java", 416 ) );
			Book found = repo.byIsbn( "978-0-13-468599-1" );
			assertNotNull( found );
			assertEquals( "Effective Java", found.getTitle() );
			assertEquals( 416, found.getPages() );
		} );
	}

	@Test
	void testFindOptionalPresent(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			repo.insert( new Book( "978-0-13-468599-1", "Effective Java", 416 ) );
			Optional<Book> found = repo.maybeByIsbn( "978-0-13-468599-1" );
			assertTrue( found.isPresent() );
			assertEquals( "Effective Java", found.get().getTitle() );
		} );
	}

	@Test
	void testFindOptionalEmpty(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			Optional<Book> found = repo.maybeByIsbn( "nonexistent" );
			assertTrue( found.isEmpty() );
		} );
	}

	@Test
	void testFindNonExistentThrows(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			assertThrows( DataException.class, () -> repo.byIsbn( "nonexistent" ) );
		} );
	}

	@Test
	void testInsertBatch(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			repo.insertAll( List.of(
					new Book( "isbn-1", "Book One", 100 ),
					new Book( "isbn-2", "Book Two", 200 )
			) );
			assertEquals( 2, repo.countAll() );
		} );
	}

	@Test
	void testUpdate(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			Book book = new Book( "isbn-u", "Original", 100 );
			repo.insert( book );
			book.setTitle( "Updated" );
			repo.update( book );
			assertEquals( "Updated", repo.byIsbn( "isbn-u" ).getTitle() );
		} );
	}

	@Test
	void testDelete(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			Book book = new Book( "isbn-d", "To Delete", 50 );
			repo.insert( book );
			assertNotNull( repo.byIsbn( "isbn-d" ) );
			repo.delete( book );
			assertThrows( DataException.class, () -> repo.byIsbn( "isbn-d" ) );
		} );
	}

	@Test
	void testSaveAsInsert(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			Book book = new Book( "isbn-s", "First", 100 );
			repo.save( book );
			assertEquals( "First", repo.byIsbn( "isbn-s" ).getTitle() );
		} );
	}

	@Test
	void testQueryWithLike(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			repo.insertAll( List.of(
					new Book( "isbn-q1", "Java Programming", 300 ),
					new Book( "isbn-q2", "Java Concurrency", 400 ),
					new Book( "isbn-q3", "Python Basics", 200 )
			) );
			List<Book> javaBooks = repo.titleLike( "Java%" );
			assertEquals( 2, javaBooks.size() );
		} );
	}

	@Test
	void testQueryWithMinPages(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			repo.insertAll( List.of(
					new Book( "isbn-p1", "Short", 50 ),
					new Book( "isbn-p2", "Medium", 200 ),
					new Book( "isbn-p3", "Long", 500 )
			) );
			List<Book> longBooks = repo.booksWithMorePages( 100 );
			assertEquals( 2, longBooks.size() );
			assertEquals( "Long", longBooks.get( 0 ).getTitle() );
		} );
	}

	@Test
	void testDeleteByCriteria(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			repo.insert( new Book( "isbn-dc", "Criteria", 100 ) );
			assertNotNull( repo.byIsbn( "isbn-dc" ) );
			repo.deleteByIsbn( "isbn-dc" );
			assertThrows( DataException.class, () -> repo.byIsbn( "isbn-dc" ) );
		} );
	}

	@Test
	void testCountAll(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			assertEquals( 0, repo.countAll() );
			repo.insert( new Book( "isbn-c1", "One", 100 ) );
			repo.insert( new Book( "isbn-c2", "Two", 200 ) );
			assertEquals( 2, repo.countAll() );
		} );
	}

	@Test
	void testFindByTitle(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _BookRepository( session );
			repo.insertAll( List.of(
					new Book( "isbn-t1", "Same Title", 100 ),
					new Book( "isbn-t2", "Same Title", 200 ),
					new Book( "isbn-t3", "Different", 300 )
			) );
			List<Book> books = repo.byTitle( "Same Title" );
			assertEquals( 2, books.size() );
		} );
	}
}
