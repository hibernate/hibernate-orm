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
	@WithClasses({ Book.class, Library.class })
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
		assertEquals( "Library.findBooks", findBooksReference.getName() );
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
		assertEquals( "Library.nativeBook", nativeBookReference.getName() );
		assertEquals( List.of( String.class ), nativeBookReference.getParameterTypes() );
		assertEquals( List.of( "isbn" ), nativeBookReference.getParameterNames() );
		assertEquals( List.of( "9781932394153" ), nativeBookReference.getArguments() );

		final Method deleteObsolete = getMethodFromMetamodelFor( Library.class, "deleteObsolete" );
		assertEquals( StatementReference.class, deleteObsolete.getReturnType() );
		final StatementReference statementReference =
				(StatementReference) deleteObsolete.invoke( null );
		assertEquals( "Library.deleteObsolete", statementReference.getName() );
		assertEquals( List.of(), statementReference.getParameterTypes() );
		assertEquals( List.of(), statementReference.getParameterNames() );
		assertEquals( List.of(), statementReference.getArguments() );
	}
}
