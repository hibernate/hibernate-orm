/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.reactive;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@CompilationTest
class ReactiveTest {
	@Test
	@WithClasses({ Publisher.class, Author.class, Address.class, Book.class, Library.class, Library2.class, RepoWithPrimary.class })
	void test() {
		final String library = getMetaModelSourceAsString( Library.class, true );
		final String library2 = getMetaModelSourceAsString( Library2.class, true );
		System.out.println( getMetaModelSourceAsString( Author.class ) );
		System.out.println( getMetaModelSourceAsString( Book.class ) );
		System.out.println( getMetaModelSourceAsString( Author.class, true ) );
		System.out.println( getMetaModelSourceAsString( Book.class, true ) );
		System.out.println( library );
		System.out.println( library2 );
		assertTrue( library.contains( "@Nonnull\n\tpublic Uni<Void> create(@Nonnull Book book)" ) );
		assertTrue( library.contains( "@Nonnull\n\tpublic Uni<Publisher> save(@Nonnull Publisher publisher)" ) );
		assertTrue( library2.contains( "@Nonnull\n\tpublic Uni<Void> deleteAll(@Nonnull List<Publisher> publishers)" ) );
		assertMetamodelClassGeneratedFor( Author.class, true );
		assertMetamodelClassGeneratedFor( Book.class, true );
		assertMetamodelClassGeneratedFor( Publisher.class, true );
		assertMetamodelClassGeneratedFor( Author.class );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Publisher.class );
		assertMetamodelClassGeneratedFor( Library.class, true );
		assertMetamodelClassGeneratedFor( Library2.class, true );
	}
}
