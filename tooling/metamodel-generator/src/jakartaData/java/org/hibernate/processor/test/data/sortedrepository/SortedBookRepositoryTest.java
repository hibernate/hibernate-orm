/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.sortedrepository;

import org.hibernate.processor.test.data.namedquery.Book;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that a companion repository (name ending with {@code $})
 * with specification parameters ({@code Sort}, {@code Order},
 * {@code Limit}, {@code Restriction}) produces valid generated code.
 */
@CompilationTest
class SortedBookRepositoryTest {
	@Test
	@WithClasses({ Book.class, SortedBookRepository.class, SortedBookRepository$.class })
	void test() {
		assertMetamodelClassGeneratedFor( SortedBookRepository.class, true );
		final String source = getMetaModelSourceAsString( SortedBookRepository.class, true );
		System.out.println( source );
		assertFalse( source.contains( "..class" ), "generated source should not contain '..class'" );
		assertFalse( source.contains( ".;" ), "generated source should not contain '.;'" );
	}
}
