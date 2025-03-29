/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hqlvalidation;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.IgnoreCompilationErrors;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertNoMetamodelClassGeneratedFor;

/**
 * @author Gavin King
 */
public class ValidationTest extends CompilationTest {
	@Test @IgnoreCompilationErrors
	@WithClasses({ Book.class, Dao1.class })
	public void testError1() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Dao1.class ) );
		assertNoMetamodelClassGeneratedFor( Book.class );
		assertNoMetamodelClassGeneratedFor( Dao1.class );
	}

	@Test @IgnoreCompilationErrors
	@WithClasses({ Book.class, Dao2.class })
	public void testError2() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Dao2.class ) );
		assertNoMetamodelClassGeneratedFor( Book.class );
		assertNoMetamodelClassGeneratedFor( Dao2.class );
	}
}
