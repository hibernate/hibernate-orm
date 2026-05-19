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
		assertFalse( repository.contains( ".setHint(\"org.hibernate.readOnly\", \"true\")" ) );
		assertFalse( repository.contains( ".setHint(\"jakarta.persistence.loadgraph\", session.getEntityGraph(\"Book.summary\"))" ) );
		assertFalse( repository.contains( ".setTimeout(Timeout.milliseconds(500))" ) );
		assertTrue( repository.contains( ".setQueryFlushMode(QueryFlushMode.NO_FLUSH)" ) );
		assertFalse( repository.contains( ".setCacheStoreMode(CacheStoreMode.BYPASS)" ) );
		assertFalse( repository.contains( ".setCacheRetrieveMode(CacheRetrieveMode.BYPASS)" ) );
		assertFalse( repository.contains( ".setLockMode(LockModeType.PESSIMISTIC_READ)" ) );
		assertFalse( repository.contains( ".setLockScope(PessimisticLockScope.EXTENDED)" ) );
		assertTrue( repository.contains( ".setHint(\"find.hint\", \"yes\")" ) );
		assertTrue( repository.contains( ".setTimeout(Timeout.milliseconds(600))" ) );
		assertTrue( repository.contains( ".setTimeout(Timeout.milliseconds(700))" ) );
		assertTrue( repository.contains( "_defaultBooksWithHql(String title)" ) );
		assertTrue( repository.contains( "_defaultBooksWithSql(String title)" ) );
		assertTrue( repository.contains( "_defaultBooksWithJakartaDataQuery(String title)" ) );
		assertTrue( repository.contains( "_defaultBooksWithJakartaQuery(String title)" ) );
		assertTrue( repository.contains( "_defaultBooksWithNativeQuery(String title)" ) );
		assertTrue( repository.contains( "TypedQueryReference<Book> _booksBy(String authorName)" ) );
		assertTrue( repository.contains( "TypedQueryReference<Book> _booksWithOptions(String title)" ) );
		assertTrue( repository.contains( "TypedQueryReference<Long> _bookCountWithNativeResultMapping(String title)" ) );
		assertTrue( repository.contains( "TypedQueryReference<Long> _countBooksWithIsbn()" ) );
		assertTrue( repository.contains( "TypedQueryReference<Boolean> _countBooksWithIsbn2()" ) );
		assertTrue( repository.contains( "TypedQueryReference<Author> _withNoOrder2()" ) );
		assertTrue( repository.contains( "SelectionSpecification.create(_booksWithJakartaQueryOrder(title))" ) );
		assertFalse( repository.contains( "SelectionSpecification.create(new StaticTypedQueryReference<>(" ) );
		assertTrue( repository.contains( "\"BookAuthorRepository.booksWithJakartaQueryOrder\"" ) );
		assertTrue( repository.contains( "private Event<? super LifecycleEvent<?>> event;" ) );
		assertTrue( repository.contains( "PreInsertEvent<Book>" ) );
		assertTrue( repository.contains( "PostInsertEvent<Book>" ) );
		assertTrue( repository.contains( "PreUpdateEvent<Book>" ) );
		assertTrue( repository.contains( "PostUpdateEvent<Book>" ) );
		assertTrue( repository.contains( "PreDeleteEvent<Book>" ) );
		assertTrue( repository.contains( "PostDeleteEvent<Book>" ) );
		assertTrue( repository.contains( "PreUpsertEvent<Book>" ) );
		assertTrue( repository.contains( "PostUpsertEvent<Book>" ) );
		assertTrue( repository.contains( "createQuery(_bookWithTitle(title))" ) );
		assertTrue( repository.contains( "createQuery(_booksWithOptions(title))" ) );
		assertTrue( repository.contains( "createNamedQuery(\"BookAuthorRepository.nativeBookWithResultMapping\", Book.class)" ) );
		assertTrue( repository.contains( "createQuery(_bookCountWithNativeResultMapping(title))" ) );
		assertTrue( repository.contains( "createQuery(_bookTitlesWithNativeResultMapping(title))" ) );
		assertTrue( repository.contains( "createQuery(_bookRowsWithNativeResultMapping(title))" ) );
		assertTrue( repository.contains( "createQuery(_countBooksWithIsbn())" ) );
		assertTrue( repository.contains( "createQuery(_countBooksWithIsbn2())" ) );
		assertTrue( repository.contains( "createQuery(_withNoOrder2())" ) );
		assertFalse( repository.contains( "createNamedQuery(\"BookAuthorRepository.countBooksWithIsbn\", long.class)" ) );
		assertFalse( repository.contains( "createNamedQuery(\"BookAuthorRepository.countBooksWithIsbn2\", boolean.class)" ) );
		assertFalse( repository.contains( "createNamedQuery(\"BookAuthorRepository.withNoOrder2\", Author.class)" ) );
		assertTrue( repository.contains( "createNamedMutationQuery(\"BookAuthorRepository.updateAuthorAddress1\")" ) );
		assertTrue( repository.contains( "createNamedMutationQuery(\"BookAuthorRepository.updateAuthorAddress2\")" ) );
		assertTrue( repository.contains( "createNamedMutationQuery(\"BookAuthorRepository.updateAuthorAddress3\")" ) );
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
