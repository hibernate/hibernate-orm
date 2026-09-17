/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.nullable;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a finder method annotated {@code @jakarta.annotation.Nullable},
 * {@code @org.jetbrains.annotations.Nullable}, or {@code @org.jspecify.annotations.Nullable}
 * is recognized as nullable, so the generated method uses {@code getSingleResultOrNull()}
 * rather than {@code getSingleResult()}.
 *
 * @see BookRepository
 */
@CompilationTest
class NullableAnnotationTest {

	@Test
	@WithClasses({ Book.class, BookRepository.class })
	void testNullableAnnotationsAreRecognized() {
		final String repository = getMetaModelSourceAsString( BookRepository.class, true );
		System.out.println( repository );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( BookRepository.class, true );

		// all three nullable annotations must be recognized, so every finder
		// method uses 'getSingleResultOrNull()' instead of 'getSingleResult()'
		assertEquals( 3, StringHelper.count( repository, ".getSingleResultOrNull()" ),
				"expected the jakarta, JetBrains, and jspecify @Nullable variants to all be recognized" );
		// findByIsbnNotNullable's return type is annotated @NonNull, not @Nullable,
		// so it must not be treated as nullable
		assertEquals( 1, StringHelper.count( repository, ".getSingleResult()" ) );
		// findByTitle is a @Find finder on a @NaturalId attribute (no nullable
		// annotation at all), exercising the NATURAL_ID finder strategy
		assertTrue( repository.contains( "public Book findByTitle(String title)" ) );
	}
}
