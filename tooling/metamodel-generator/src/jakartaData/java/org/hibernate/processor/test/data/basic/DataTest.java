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
		final String bookMetamodel = getMetaModelSourceAsString( Book.class, true );
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
		assertTrue( repository.contains( ".find(Book.class, isbn, Timeout.milliseconds(650), CacheRetrieveMode.BYPASS)" ) );
		assertTrue( repository.contains( "_key.put(\"title\", title);" ) );
		assertTrue( repository.contains( "_key.put(\"publicationDate\", publicationDate);" ) );
		assertTrue( repository.contains( ".find(Book.class, _key, KeyType.NATURAL, Timeout.milliseconds(550), "
				+ "LockModeType.PESSIMISTIC_READ)" ) );
		assertFalse( repository.contains( ".byId(" ) );
		assertFalse( repository.contains( ".byNaturalId(" ) );
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
		assertTrue( queryMetamodel.contains( "\"org.hibernate.processor.test.data.basic.BookAuthorRepository"
				+ "#booksWithJakartaQueryOrder(java.lang.String,jakarta.data.Order)\"" ) );
		assertFalse( repository.contains( "private Event<LifecycleEvent<?>> event;" ) );
		assertFalse( repository.contains( "PreInsertEvent<Book>" ) );
		assertFalse( repository.contains( "PostUpsertEvent<Book>" ) );
		assertTrue( bookMetamodel.contains( "@EntityListener" ) );
		assertTrue( bookMetamodel.contains( "public class _Book" ) );
		assertTrue( bookMetamodel.contains( "private Event<LifecycleEvent<?>> event;" ) );
		assertTrue( bookMetamodel.contains( "PreInsertEvent<Book>" ) );
		assertTrue( bookMetamodel.contains( "PostInsertEvent<Book>" ) );
		assertTrue( bookMetamodel.contains( "PreUpdateEvent<Book>" ) );
		assertTrue( bookMetamodel.contains( "PostUpdateEvent<Book>" ) );
		assertTrue( bookMetamodel.contains( "PreDeleteEvent<Book>" ) );
		assertTrue( bookMetamodel.contains( "PostDeleteEvent<Book>" ) );
		assertTrue( bookMetamodel.contains( "PreUpsertEvent<Book>" ) );
		assertTrue( bookMetamodel.contains( "PostUpsertEvent<Book>" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.bookWithTitle(title))" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.booksWithOptions(title))" ) );
		assertTrue( repository.contains( "createNamedQuery(\"org.hibernate.processor.test.data.basic"
				+ ".BookAuthorRepository#nativeBookWithResultMapping(jakarta.persistence.EntityManager,java.lang.String)\", "
				+ "Book.class)" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.bookCountWithNativeResultMapping(title))" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.bookTitlesWithNativeResultMapping(title))" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.bookRowsWithNativeResultMapping(title))" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.countBooksWithIsbn())" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.countBooksWithIsbn2())" ) );
		assertTrue( repository.contains( "createQuery(BookAuthorRepository_.withNoOrder2())" ) );
		assertTrue( repository.contains( "Stream<Author> allAuthors(@Nonnull Order<Author> order)" ) );
		assertTrue( repository.contains( "applyOrder(order, _query, _entity, _builder);" ) );
		assertFalse( repository.contains( "order.apply(_query, _entity, _builder);" ) );
		assertFalse( repository.contains( "createNamedQuery(\"BookAuthorRepository.countBooksWithIsbn\", long.class)" ) );
		assertFalse( repository.contains( "createNamedQuery(\"BookAuthorRepository.countBooksWithIsbn2\", boolean.class)" ) );
		assertFalse( repository.contains( "createNamedQuery(\"BookAuthorRepository.withNoOrder2\", Author.class)" ) );
		assertTrue( repository.contains( "createStatement(BookAuthorRepository_.updateAuthorAddress1(id, name))" ) );
		assertTrue( repository.contains( "createStatement(BookAuthorRepository_.updateAuthorAddress2(id, name))" ) );
		assertTrue( repository.contains( "createStatement(BookAuthorRepository_.updateAuthorAddress3(id, name))" ) );
		assertFalse( repository.contains( "createNamedMutationQuery(\"BookAuthorRepository.updateAuthorAddress" ) );
		assertFalse( repository.contains( "_defaultDeleteWithJakartaQuery(String title)" ) );
		assertTrue( queryMetamodel.contains( "defaultDeleteWithJakartaQuery(String title)" ) );
		assertTrue( queryMetamodel.contains( "\"org.hibernate.processor.test.data.basic.BookAuthorRepository"
				+ "#defaultBooksWithJakartaQuery(java.lang.String)\"" ) );
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
