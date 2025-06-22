/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.namedquery;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQueryReference;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfNameFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getFieldFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gavin King
 */
public class AuxiliaryTest extends CompilationTest {
	@Test
	@WithClasses({ Book.class, Main.class })
	public void test() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Main.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Book.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Main.class );
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"QUERY_BOOK_BY_TITLE",
				"Missing named query attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"QUERY_BOOK_BY_ISBN",
				"Missing named query attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"QUERY_BOOK_NATIVE_QUERY",
				"Missing named native query attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"MAPPING_BOOK_NATIVE_QUERY_RESULT",
				"Missing result set mapping."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"PROFILE_FETCH_ONE",
				"Missing fetch profile attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"PROFILE_FETCH_TWO",
				"Missing fetch profile attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"PROFILE_DUMMY_FETCH",
				"Missing fetch profile attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"MAPPING_RESULT_SET_MAPPING_ONE",
				"Missing fetch profile attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"MAPPING_RESULT_SET_MAPPING_TWO",
				"Missing fetch profile attribute."
		);
		assertPresenceOfNameFieldInMetamodelFor(
				Main.class,
				"QUERY__SYSDATE_",
				"Missing fetch profile attribute."
		);
		assertPresenceOfMethodInMetamodelFor(
				Main.class,
				"bookByIsbn",
				EntityManager.class,
				String.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Main.class,
				"bookByTitle",
				EntityManager.class,
				String.class
		);

		assertPresenceOfNameFieldInMetamodelFor(
				Book.class,
				"GRAPH_ENTITY_GRAPH",
				"Missing fetch profile attribute."
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"findByTitle",
				EntityManager.class,
				String.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"findByTitleAndType",
				EntityManager.class,
				String.class,
				Type.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"getTitles",
				EntityManager.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"getUpperLowerTitles",
				EntityManager.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"typeOfBook",
				EntityManager.class,
				String.class
		);
		assertPresenceOfMethodInMetamodelFor(
				Book.class,
				"crazy",
				EntityManager.class,
				Object.class,
				Object.class
		);

		checkTypedQueryReference( "QUERY_TITLES_WITH_ISBNS", "_titlesWithIsbns_", Object[].class );
		checkTypedQueryReference( "QUERY_TITLES_AND_ISBNS_AS_RECORD", "_titlesAndIsbnsAsRecord_", TitleAndIsbn.class );
		checkTypedQueryReference( "QUERY_TYPE_OF_BOOK", "__typeOfBook_", Type.class );
		checkTypedQueryReference( "QUERY_GET_TITLES", "__getTitles_", String.class );
		checkTypedQueryReference( "QUERY_GET_UPPER_LOWER_TITLES", "__getUpperLowerTitles_", Object[].class );
		checkTypedQueryReference( "QUERY_BOOKS_BY_TITLE", "_booksByTitle_", Book.class );
		checkTypedQueryReference( "QUERY_BOOKS_BY_TITLE_VERBOSE", "_booksByTitleVerbose_", Book.class );
	}

	private static void checkTypedQueryReference(String NAME, String name, Class<?> type) {
		assertPresenceOfNameFieldInMetamodelFor(
				Book.class,
				NAME,
				"Missing named query attribute."
		);
		assertPresenceOfFieldInMetamodelFor(
				Book.class,
				name,
				"Missing typed query reference."
		);
		ParameterizedType fieldType = (ParameterizedType)
				getFieldFromMetamodelFor( Book.class, name )
						.getGenericType();
		assertEquals(TypedQueryReference.class, fieldType.getRawType());
		assertEquals( type, fieldType.getActualTypeArguments()[0]);
	}
}
