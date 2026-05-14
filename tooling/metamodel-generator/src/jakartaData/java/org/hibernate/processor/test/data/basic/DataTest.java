/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.basic;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertNoMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@CompilationTest
class DataTest {
	@Test
	@WithClasses({ Author.class, Book.class, BookAuthorRepository.class, IdOperations.class, Concrete.class, Thing.class })
	void test() {
		System.out.println( getMetaModelSourceAsString( Author.class ) );
		System.out.println( getMetaModelSourceAsString( Book.class ) );
		System.out.println( getMetaModelSourceAsString( Author.class, true ) );
		System.out.println( getMetaModelSourceAsString( Book.class, true ) );
		final String repository = getMetaModelSourceAsString( BookAuthorRepository.class );
		System.out.println( repository );
		assertTrue( repository.contains( ".setHint(\"org.hibernate.readOnly\", \"true\")" ) );
		assertTrue( repository.contains( ".setHint(\"jakarta.persistence.loadgraph\", session.getEntityGraph(\"Book.summary\"))" ) );
		assertTrue( repository.contains( ".setTimeout(Timeout.milliseconds(500))" ) );
		assertTrue( repository.contains( ".setQueryFlushMode(QueryFlushMode.NO_FLUSH)" ) );
		assertTrue( repository.contains( ".setCacheStoreMode(CacheStoreMode.BYPASS)" ) );
		assertTrue( repository.contains( ".setCacheRetrieveMode(CacheRetrieveMode.BYPASS)" ) );
		assertTrue( repository.contains( ".setLockMode(LockModeType.PESSIMISTIC_READ)" ) );
		assertTrue( repository.contains( ".setLockScope(PessimisticLockScope.EXTENDED)" ) );
		assertTrue( repository.contains( ".setHint(\"find.hint\", \"yes\")" ) );
		assertTrue( repository.contains( ".setTimeout(Timeout.milliseconds(600))" ) );
		assertTrue( repository.contains( ".setTimeout(Timeout.milliseconds(700))" ) );
		assertTrue( repository.contains( "_defaultBooksWithHql(String title)" ) );
		assertTrue( repository.contains( "_defaultBooksWithSql(String title)" ) );
		assertTrue( repository.contains( "_defaultBooksWithJakartaDataQuery(String title)" ) );
		assertTrue( repository.contains( "_defaultBooksWithJakartaQuery(String title)" ) );
		assertTrue( repository.contains( "_defaultBooksWithNativeQuery(String title)" ) );
		assertTrue( repository.contains(
				"createNativeQuery(NATIVE_BOOK_WITH_RESULT_MAPPING_String, entity(Book.class, field(Book.class, String.class, \"isbn\", \"book_isbn\")))" ) );
		assertTrue( repository.contains(
				"createNativeQuery(BOOK_COUNT_WITH_NATIVE_RESULT_MAPPING_String, column(\"book_count\", Long.class))" ) );
		assertTrue( repository.contains(
				"createNativeQuery(BOOK_TITLES_WITH_NATIVE_RESULT_MAPPING_String, constructor(BookTitle.class, column(\"book_title\", String.class)))" ) );
		assertTrue( repository.contains( "_defaultDeleteWithJakartaQuery(String title)" ) );
		assertTrue( repository.contains( "\"BookAuthorRepository.defaultBooksWithJakartaQuery\"" ) );
		assertFalse( repository.contains( "@Override\n\tpublic List<Book> defaultBooksWithJakartaQuery(String title)" ) );
		assertMetamodelClassGeneratedFor( Author.class, true );
		assertMetamodelClassGeneratedFor( Book.class, true );
		assertMetamodelClassGeneratedFor( Author.class );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( BookAuthorRepository.class );
		assertMetamodelClassGeneratedFor( Concrete.class );
		assertNoMetamodelClassGeneratedFor( IdOperations.class );
	}
}
