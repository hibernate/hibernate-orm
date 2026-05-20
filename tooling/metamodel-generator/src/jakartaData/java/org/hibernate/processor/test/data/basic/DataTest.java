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
		final String repository = getMetaModelSourceAsString( BookAuthorRepository.class, true );
		final String queryMetamodel = getMetaModelSourceAsString( BookAuthorRepository.class );
		System.out.println( repository );
		System.out.println( queryMetamodel );
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
		assertFalse( repository.contains( "TypedQueryReference<" ) );
		assertFalse( repository.contains( "_defaultBooksWithHql(String title)" ) );
		assertFalse( repository.contains( "_defaultBooksWithSql(String title)" ) );
		assertFalse( repository.contains( "_defaultBooksWithJakartaDataQuery(String title)" ) );
		assertFalse( repository.contains( "_defaultBooksWithJakartaQuery(String title)" ) );
		assertFalse( repository.contains( "_defaultBooksWithNativeQuery(String title)" ) );
		assertTrue( queryMetamodel.contains( "defaultBooksWithHql(String title)" ) );
		assertTrue( queryMetamodel.contains( "defaultBooksWithSql(String title)" ) );
		assertTrue( queryMetamodel.contains( "defaultBooksWithJakartaDataQuery(String title)" ) );
		assertTrue( queryMetamodel.contains( "defaultBooksWithJakartaQuery(String title)" ) );
		assertTrue( queryMetamodel.contains( "defaultBooksWithNativeQuery(String title)" ) );
		assertTrue( queryMetamodel.contains( "TypedQueryReference<Book> booksBy(String authorName)" ) );
		assertFalse( queryMetamodel.contains( "TypedQueryReference<Book> _booksBy(String authorName)" ) );
		assertTrue( queryMetamodel.contains( "TypedQueryReference<Book> booksWithOptions(String title)" ) );
		assertTrue( queryMetamodel.contains( "TypedQueryReference<Long> bookCountWithNativeResultMapping(String title)" ) );
		assertTrue( queryMetamodel.contains( "TypedQueryReference<Long> countBooksWithIsbn()" ) );
		assertTrue( queryMetamodel.contains( "TypedQueryReference<Boolean> countBooksWithIsbn2()" ) );
		assertTrue( queryMetamodel.contains( "TypedQueryReference<Author> withNoOrder2()" ) );
		assertTrue( repository.contains( "SelectionSpecification.create(BookAuthorRepository_.booksWithJakartaQueryOrder(title))" ) );
		assertFalse( repository.contains( "SelectionSpecification.create(new StaticTypedQueryReference<>(" ) );
		assertTrue( queryMetamodel.contains( "\"BookAuthorRepository.booksWithJakartaQueryOrder\"" ) );
		assertTrue( repository.contains( "private Event<? super LifecycleEvent<?>> event;" ) );
		assertTrue( repository.contains( "PreInsertEvent<Book>" ) );
		assertTrue( repository.contains( "PostInsertEvent<Book>" ) );
		assertTrue( repository.contains( "PreUpdateEvent<Book>" ) );
		assertTrue( repository.contains( "PostUpdateEvent<Book>" ) );
		assertTrue( repository.contains( "PreDeleteEvent<Book>" ) );
		assertTrue( repository.contains( "PostDeleteEvent<Book>" ) );
		assertTrue( repository.contains( "PreUpsertEvent<Book>" ) );
		assertTrue( repository.contains( "PostUpsertEvent<Book>" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.bookWithTitle(title))" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.booksWithOptions(title))" ) );
		assertTrue( repository.contains( "createNamedQuery(\"BookAuthorRepository.nativeBookWithResultMapping\", Book.class)" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.bookCountWithNativeResultMapping(title))" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.bookTitlesWithNativeResultMapping(title))" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.bookRowsWithNativeResultMapping(title))" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.countBooksWithIsbn())" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.countBooksWithIsbn2())" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.withNoOrder2())" ) );
		assertFalse( repository.contains( "createNamedQuery(\"BookAuthorRepository.countBooksWithIsbn\", long.class)" ) );
		assertFalse( repository.contains( "createNamedQuery(\"BookAuthorRepository.countBooksWithIsbn2\", boolean.class)" ) );
		assertFalse( repository.contains( "createNamedQuery(\"BookAuthorRepository.withNoOrder2\", Author.class)" ) );
		assertTrue( repository.contains( "createStatement(BookAuthorRepository_.updateAuthorAddress1(id, name))" ) );
		assertTrue( repository.contains( "createStatement(BookAuthorRepository_.updateAuthorAddress2(id, name))" ) );
		assertTrue( repository.contains( "createStatement(BookAuthorRepository_.updateAuthorAddress3(id, name))" ) );
		assertFalse( repository.contains( "createNamedMutationQuery(\"BookAuthorRepository.updateAuthorAddress" ) );
		assertFalse( repository.contains( "_defaultDeleteWithJakartaQuery(String title)" ) );
		assertTrue( queryMetamodel.contains( "defaultDeleteWithJakartaQuery(String title)" ) );
		assertTrue( queryMetamodel.contains( "\"BookAuthorRepository.defaultBooksWithJakartaQuery\"" ) );
		assertFalse( repository.contains( "@Override\n\tpublic List<Book> defaultBooksWithJakartaQuery(String title)" ) );
		assertMetamodelClassGeneratedFor( Author.class, true );
		assertMetamodelClassGeneratedFor( Book.class, true );
		assertMetamodelClassGeneratedFor( Author.class );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( BookAuthorRepository.class, true );
		assertMetamodelClassGeneratedFor( BookAuthorRepository.class );
		assertMetamodelClassGeneratedFor( Concrete.class, true );
		assertMetamodelClassGeneratedFor( Concrete.class );
		assertNoMetamodelClassGeneratedFor( IdOperations.class );
	}
}
