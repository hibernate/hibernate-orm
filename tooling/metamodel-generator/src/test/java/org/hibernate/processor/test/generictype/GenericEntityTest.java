/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.generictype;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static java.lang.System.out;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

/**
 * @author Hardy Ferentschik
 */
public class GenericEntityTest extends CompilationTest {

	@Test
	@WithClasses(Generic.class)
	public void testGeneric() {
		out.println( getMetaModelSourceAsString( Generic.class ) );
		assertMetamodelClassGeneratedFor( Generic.class );
	}
}
