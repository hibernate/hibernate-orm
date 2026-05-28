/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.model.Book;
import org.hibernate.processor.test.integ.repository.CompanionBookRepository;
import org.hibernate.processor.test.integ.repository._CompanionBookRepository;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = { Book.class, CompanionBookRepository.class },
		annotatedClassNames = "org.hibernate.processor.test.integ.repository.CompanionBookRepository$"
)
@SessionFactory
class CompanionBookRepositoryTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testInsertAndFindFromMainInterface(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _CompanionBookRepository( session );
			repo.insert( new Book( "isbn-1", "Effective Java", 416 ) );
			Book found = repo.byIsbn( "isbn-1" );
			assertNotNull( found );
			assertEquals( "Effective Java", found.getTitle() );
		} );
	}

	@Test
	void testFindByTitleFromMainInterface(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _CompanionBookRepository( session );
			repo.insert( new Book( "isbn-1", "Java", 100 ) );
			repo.insert( new Book( "isbn-2", "Java", 200 ) );
			repo.insert( new Book( "isbn-3", "Python", 300 ) );
			List<Book> books = repo.byTitle( "Java" );
			assertEquals( 2, books.size() );
		} );
	}

	@Test
	void testCountAllFromCompanionOverride(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _CompanionBookRepository( session );
			assertEquals( 0, repo.countAll() );
			repo.insert( new Book( "isbn-1", "One", 100 ) );
			repo.insert( new Book( "isbn-2", "Two", 200 ) );
			assertEquals( 2, repo.countAll() );
		} );
	}
}
