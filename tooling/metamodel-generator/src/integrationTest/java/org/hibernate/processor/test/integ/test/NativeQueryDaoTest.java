package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.dao.NativeQueryDao;
import org.hibernate.processor.test.integ.dao.NativeQueryDao.BookRecord;
import org.hibernate.processor.test.integ.dao._NativeQueryDao;
import org.hibernate.processor.test.integ.model.Book;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = { Book.class, NativeQueryDao.class })
@SessionFactory
class NativeQueryDaoTest {

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
	void testNativeQueryWithSession(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var dao = new _NativeQueryDao( session );
			assertEquals(
					List.of( new BookRecord( "isbn-1", "Java" ), new BookRecord( "isbn-2", "Java" ) ),
					dao.findByTitle( "Java" )
			);
			assertEquals( List.of(), dao.findByTitle( "Missing" ) );
		} );
	}

	@Test
	void testSqlWithSession(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var dao = new _NativeQueryDao( session );
			assertEquals(
					List.of( new BookRecord( "isbn-1", "Java" ), new BookRecord( "isbn-2", "Java" ) ),
					dao.findByTitleSql( "Java" )
			);
			assertEquals( List.of(), dao.findByTitleSql( "Missing" ) );
		} );
	}
}
