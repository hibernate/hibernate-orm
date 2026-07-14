/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.wildcard;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that an extends-bounded wildcard list parameter
 * {@code List<? extends X>} is recognized as a multivalued {@code in}
 * parameter, exactly like a plain {@code List<X>} parameter.
 *
 * @see BookRepository
 */
@CompilationTest
class ListExtendsParameterTest {

	@Test
	@WithClasses({ Book.class, BookRepository.class })
	void testWildcardListParameterIsMultivalued() {
		final String repository = getMetaModelSourceAsString( BookRepository.class, true );
		System.out.println( repository );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( BookRepository.class, true );

		// Both the plain List<String> and the List<? extends String> parameter
		// must be rendered as an 'in' condition, so '.in(genre)' appears once
		// per finder method.
		assertEquals( 2, StringHelper.count( repository, ".in(genre)" ),
				"expected an 'in' condition for both the List<String> and the "
						+ "List<? extends String> parameter" );
	}
}
