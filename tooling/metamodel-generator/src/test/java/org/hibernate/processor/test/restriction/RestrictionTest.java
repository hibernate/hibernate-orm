/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.restriction;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

public class RestrictionTest extends CompilationTest {
	@Test
	@WithClasses({Book.class, Bookshelf.class})
	public void testRestriction() {
		System.out.println( getMetaModelSourceAsString( Bookshelf.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Bookshelf.class );
	}
}
