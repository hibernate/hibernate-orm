/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.namedquery;

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
class NamedQueryTest {
	@Test
	@WithClasses({ Author.class, Book.class, BookAuthorRepository.class, BookAuthorRepository$.class })
	void test() {
		System.out.println( getMetaModelSourceAsString( Author.class ) );
		System.out.println( getMetaModelSourceAsString( Book.class ) );
		System.out.println( getMetaModelSourceAsString( Author.class, true ) );
		System.out.println( getMetaModelSourceAsString( Book.class, true ) );
		final String repository = getMetaModelSourceAsString( BookAuthorRepository.class, true );
		System.out.println( repository );
		assertTrue( repository.contains(
				"entityAgent.createQuery(BookAuthorRepository_.findByTitleLike(title))" ) );
		assertTrue( repository.contains(
				"entityAgent.createQuery(BookAuthorRepository_.findByTypeIn(types))" ) );
		assertFalse( repository.contains( "createNamedQuery(\"org.hibernate.processor.test.data.namedquery"
				+ ".BookAuthorRepository$#findByTitleLike(java.lang.String)\", Book.class)" ) );
		assertFalse( repository.contains( "createNamedQuery(\"org.hibernate.processor.test.data.namedquery"
				+ ".BookAuthorRepository$#findByTypeIn(java.util.Set)\", Book.class)" ) );
		assertMetamodelClassGeneratedFor( Author.class, true );
		assertMetamodelClassGeneratedFor( Book.class, true );
		assertMetamodelClassGeneratedFor( Author.class );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( BookAuthorRepository.class, true );
		assertNoMetamodelClassGeneratedFor( BookAuthorRepository$.class );
	}
}
