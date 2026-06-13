/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.staticquery;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.StatementReference;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQueryReference;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMethodFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class StaticQueryTest {
	@Test
	@WithClasses({ Book.class, Library.class, NotARepo.class })
	void testStaticQueryReferenceMethods() throws ReflectiveOperationException {
		System.out.println( TestUtil.getMetaModelSourceAsString( Library.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Library.class );

		final Method findBooks = getMethodFromMetamodelFor( Library.class, "findBooks", String.class );
		final ParameterizedType findBooksType = (ParameterizedType) findBooks.getGenericReturnType();
		assertEquals( TypedQueryReference.class, findBooksType.getRawType() );
		assertEquals( Book.class, findBooksType.getActualTypeArguments()[0] );

		final TypedQueryReference<?> findBooksReference =
				(TypedQueryReference<?>) findBooks.invoke( null, "Hibernate" );
		assertEquals( Library.class.getName() + "#findBooks(java.lang.String)", findBooksReference.getName() );
		assertEquals( Book.class, findBooksReference.getResultType() );
		assertEquals( List.of( String.class ), findBooksReference.getParameterTypes() );
		assertEquals( List.of( "title" ), findBooksReference.getParameterNames() );
		assertEquals( List.of( "Hibernate" ), findBooksReference.getArguments() );
		assertEquals( "Book.summary", findBooksReference.getEntityGraphName() );
		assertEquals( "true", findBooksReference.getHints().get( "org.hibernate.readOnly" ) );
		assertTrue( findBooksReference.getOptions().contains( CacheStoreMode.BYPASS ) );
		assertTrue( findBooksReference.getOptions().contains( QueryFlushMode.FLUSH ) );
		assertTrue( findBooksReference.getOptions().contains( Timeout.milliseconds( 500 ) ) );

		final Method nativeBook = getMethodFromMetamodelFor( Library.class, "nativeBook", String.class );
		final ParameterizedType nativeBookType = (ParameterizedType) nativeBook.getGenericReturnType();
		assertEquals( TypedQueryReference.class, nativeBookType.getRawType() );
		assertEquals( Book.class, nativeBookType.getActualTypeArguments()[0] );

		final TypedQueryReference<?> nativeBookReference =
				(TypedQueryReference<?>) nativeBook.invoke( null, "9781932394153" );
		assertEquals( Library.class.getName() + "#nativeBook(java.lang.String)", nativeBookReference.getName() );
		assertEquals( List.of( String.class ), nativeBookReference.getParameterTypes() );
		assertEquals( List.of( "isbn" ), nativeBookReference.getParameterNames() );
		assertEquals( List.of( "9781932394153" ), nativeBookReference.getArguments() );

		final Method deleteObsolete = getMethodFromMetamodelFor( Library.class, "deleteObsolete" );
		assertEquals( StatementReference.class, deleteObsolete.getReturnType() );
		final StatementReference statementReference =
				(StatementReference) deleteObsolete.invoke( null );
		assertEquals( Library.class.getName() + "#deleteObsolete()", statementReference.getName() );
		assertEquals( List.of(), statementReference.getParameterTypes() );
		assertEquals( List.of(), statementReference.getParameterNames() );
		assertEquals( List.of(), statementReference.getArguments() );
	}

	@Test
	@WithClasses({ Book.class, NotARepo.class })
	void nonRepositoryProjectionQueryReferenceUsesExplicitRootEntity() throws ReflectiveOperationException {
		final var metamodel = TestUtil.getMetaModelSourceAsString( NotARepo.class );
		assertTrue( metamodel.contains( "TypedQueryReference<Book> summaries()" ) );
		assertTrue( metamodel.contains( "Book.class" ) );

		final Method books = getMethodFromMetamodelFor( NotARepo.class, "books" );
		final ParameterizedType booksType = (ParameterizedType) books.getGenericReturnType();
		assertEquals( TypedQueryReference.class, booksType.getRawType() );
		assertEquals( Book.class, booksType.getActualTypeArguments()[0] );

		final Method summaries = getMethodFromMetamodelFor( NotARepo.class, "summaries" );
		final ParameterizedType summariesType = (ParameterizedType) summaries.getGenericReturnType();
		assertEquals( TypedQueryReference.class, summariesType.getRawType() );
		assertEquals( Book.class, summariesType.getActualTypeArguments()[0] );

		final TypedQueryReference<?> summariesReference =
				(TypedQueryReference<?>) summaries.invoke( null );
		assertEquals( NotARepo.class.getName() + "#summaries()", summariesReference.getName() );
		assertEquals( Book.class, summariesReference.getResultType() );
	}
}
