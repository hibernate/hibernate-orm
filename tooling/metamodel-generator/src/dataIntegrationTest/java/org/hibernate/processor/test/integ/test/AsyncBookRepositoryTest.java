package org.hibernate.processor.test.integ.test;

import java.util.List;

import org.hibernate.processor.test.integ.model.Book;
import org.hibernate.processor.test.integ.repository.AsyncBookRepository;
import org.hibernate.processor.test.integ.repository._AsyncBookRepository;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = { Book.class, AsyncBookRepository.class })
@SessionFactory
class AsyncBookRepositoryTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testQueryWithoutFromClause(SessionFactoryScope scope) throws Exception {
		scope.inStatelessTransaction( session -> {
			session.insert( new Book( "isbn-1", "Async", 100 ) );
			session.insert( new Book( "isbn-2", "Async", 200 ) );
			session.insert( new Book( "isbn-3", "Other", 300 ) );
		} );
		scope.inStatelessTransaction( session -> {
			var repo = new _AsyncBookRepository( session );
			List<Book> books = repo.asyncListByTitle( "Async" ).toCompletableFuture().join();
			assertEquals( List.of( "isbn-1", "isbn-2" ), books.stream().map( Book::getIsbn ).toList() );
			assertEquals( "isbn-3", repo.asyncByTitle( "Other" ).toCompletableFuture().join().getIsbn() );
		} );
	}
}
