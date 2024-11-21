/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.namedentity;

import jakarta.persistence.TypedQueryReference;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getFieldFromMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NamedEntityTest extends CompilationTest {

	@Test
	@WithClasses(Book.class)
	public void test() {
		System.out.println( getMetaModelSourceAsString( Book.class ) );

		assertMetamodelClassGeneratedFor( Book.class );

		assertPresenceOfFieldInMetamodelFor( Book.class, "QUERY_FIND_ALL_BOOKS" );
		final Field field = getFieldFromMetamodelFor( Book.class, "_findAllBooks_" );
		assertEquals( TypedQueryReference.class, field.getType() );
		final Type genericType = field.getGenericType();
		assertTrue( genericType instanceof ParameterizedType );
		final ParameterizedType parameterizedType = (ParameterizedType) genericType;
		assertEquals( Book.class, parameterizedType.getActualTypeArguments()[0] );
	}
}
