package org.hibernate.processor.test.nativequery;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQueryReference;
import org.hibernate.Session;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;
import static org.hibernate.processor.test.util.TestUtil.getMethodFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class NativeQuerySessionTest {

	@Test
	@WithClasses(NativeQueries.class)
	void nativeQueryWithoutSessionGeneratesQueryReference() throws Exception {
		final var method = getMethodFromMetamodelFor( NativeQueries.class, "findByTitle", String.class );
		final var returnType = (ParameterizedType) method.getGenericReturnType();
		assertEquals( TypedQueryReference.class, returnType.getRawType() );
		assertEquals( NativeQueries.BookRecord.class.getName(), returnType.getActualTypeArguments()[0].getTypeName() );
	}

	@Test
	@WithClasses({ NativeQueries.class, NativeQueriesWithSession.class })
	void nativeQueryWithSessionGeneratesBlockingImplementation() throws Exception {
		final var implementation = getMetamodelClassFor( NativeQueriesWithSession.class, true );
		assertTrue( NativeQueriesWithSession.class.isAssignableFrom( implementation ) );
		assertEquals( Session.class, implementation.getDeclaredConstructor( Session.class ).getParameterTypes()[0] );
		assertEquals( List.class, implementation.getDeclaredMethod( "findByTitle", String.class ).getReturnType() );
		final var queryReference = getMethodFromMetamodelFor( NativeQueriesWithSession.class, "findByTitle", String.class );
		assertEquals( TypedQueryReference.class, queryReference.getReturnType() );
	}

	@Test
	@WithClasses({ NativeQueries.class, SqlQueriesWithSession.class })
	void sqlQueryWithSessionGeneratesBlockingImplementation() throws Exception {
		final var implementation = getMetamodelClassFor( SqlQueriesWithSession.class, true );
		assertTrue( SqlQueriesWithSession.class.isAssignableFrom( implementation ) );
		assertEquals( Session.class, implementation.getDeclaredConstructor( Session.class ).getParameterTypes()[0] );
		assertEquals( List.class, implementation.getDeclaredMethod( "findByTitle", String.class ).getReturnType() );
	}

	@Test
	@WithClasses({ NativeQueries.class, SqlQueries.class })
	void sqlQueryWithoutSessionGeneratesStaticQueryMethod() throws Exception {
		final var method = getMethodFromMetamodelFor( SqlQueries.class, "findByTitle", EntityManager.class, String.class );
		assertTrue( Modifier.isStatic( method.getModifiers() ) );
		assertEquals( List.class, method.getReturnType() );
	}
}
