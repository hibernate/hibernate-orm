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

/**
 * @author Gavin King
 */
@CompilationTest
class ReactiveTest {
	@Test
	@WithClasses({ Publisher.class, Author.class, Address.class, Book.class, Library.class, Library2.class, RepoWithPrimary.class })
	void test() {
		System.out.println( getMetaModelSourceAsString( Author.class ) );
		System.out.println( getMetaModelSourceAsString( Book.class ) );
		System.out.println( getMetaModelSourceAsString( Author.class, true ) );
		System.out.println( getMetaModelSourceAsString( Book.class, true ) );
		System.out.println( getMetaModelSourceAsString( Library.class ) );
		System.out.println( getMetaModelSourceAsString( Library2.class ) );
		assertMetamodelClassGeneratedFor( Author.class, true );
		assertMetamodelClassGeneratedFor( Book.class, true );
		assertMetamodelClassGeneratedFor( Publisher.class, true );
		assertMetamodelClassGeneratedFor( Author.class );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Publisher.class );
		assertMetamodelClassGeneratedFor( Library.class );
		assertMetamodelClassGeneratedFor( Library2.class );
	}
}
