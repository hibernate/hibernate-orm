/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.cursoredpage;

import org.hibernate.processor.test.data.namedquery.Author;
import org.hibernate.processor.test.data.namedquery.Book;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class CursoredBookRepositoryTest {
	@Test
	@WithClasses({ Book.class, Author.class, CursoredBookRepository.class })
	void test() {
		assertMetamodelClassGeneratedFor( CursoredBookRepository.class );
		final String source = getMetaModelSourceAsString( CursoredBookRepository.class );
		System.out.println( source );
		assertTrue( source.contains( "import java.util.List" ) );
	}
}
