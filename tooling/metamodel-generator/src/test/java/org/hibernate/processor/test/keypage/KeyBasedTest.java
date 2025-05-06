/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.keypage;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Gavin King
 */
public class KeyBasedTest extends CompilationTest {
	@Test
	@WithClasses({ Book.class, Dao.class, Queries.class })
	public void testQueryMethod() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Dao.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Queries.class ) );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Dao.class );
		assertMetamodelClassGeneratedFor( Queries.class );
	}
}
