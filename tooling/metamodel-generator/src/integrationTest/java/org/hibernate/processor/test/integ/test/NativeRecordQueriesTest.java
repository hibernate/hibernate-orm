package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.dao.NativeRecordQueries;
import org.hibernate.processor.test.integ.dao.NativeRecordQueries.BookRecord;
import org.hibernate.processor.test.integ.dao.NativeRecordQueries_;
import org.hibernate.processor.test.integ.model.Book;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @see <a href="https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4#result-class-native-sql">Jakarta Persistence 4.0-M4, section 3.15.14: Result Classes for SQL Queries</a>
 */
@DomainModel(annotatedClasses = { Book.class, NativeRecordQueries.class })
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20970")
class NativeRecordQueriesTest {

	private static final List<BookRecord> EXPECTED =
			List.of( new BookRecord( "isbn-1", "Java" ), new BookRecord( "isbn-2", "Java" ) );

	@BeforeEach
	void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book( "isbn-1", "Java", "Author A", 100 ) );
			session.persist( new Book( "isbn-2", "Java", "Author B", 200 ) );
			session.persist( new Book( "isbn-3", "Python", "Author C", 300 ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void nativeQueryWithoutMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( EXPECTED,
					session.createQuery( NativeRecordQueries_.findByTitle( "Java" ) ).getResultList() );
		} );
	}

	@Test
	void nativeQueryWithConstructorMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( EXPECTED,
					session.createQuery( NativeRecordQueries_.findByTitleMapped( "Java" ) ).getResultList() );
		} );
	}

	@Test
	void namedQueryWithoutMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( EXPECTED,
					session.createNamedQuery(
							NativeRecordQueries.class.getName() + "#findByTitle(java.lang.String)",
							BookRecord.class
					)
							.setParameter( 1, "Java" )
							.getResultList() );
		} );
	}

	@Test
	void namedNativeQueryWithResultClass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( EXPECTED,
					session.createNamedQuery( "booksAsRecords", BookRecord.class )
							.setParameter( 1, "Java" )
							.getResultList() );
		} );
	}

	@Test
	void namedNativeQueryWithInferredResultClass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( EXPECTED,
					session.createNamedQuery( "booksAsRecords" )
							.setParameter( 1, "Java" )
							.getResultList() );
		} );
	}

	@Test
	void dynamicNativeQueryWithoutMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( EXPECTED,
					session.createNativeQuery(
							"select isbn, title from integ_books where title = ?1 order by isbn",
							BookRecord.class
					)
							.setParameter( 1, "Java" )
							.getResultList() );
		} );
	}
}
