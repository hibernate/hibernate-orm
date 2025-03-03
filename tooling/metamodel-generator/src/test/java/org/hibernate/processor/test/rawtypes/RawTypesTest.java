/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.rawtypes;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Emmanuel Bernard
 */
public class RawTypesTest extends CompilationTest {

	@Test
	@WithClasses({ DeskWithRawType.class, EmployeeWithRawType.class })
	public void testGenerics() {
		assertMetamodelClassGeneratedFor( DeskWithRawType.class );
		assertMetamodelClassGeneratedFor( EmployeeWithRawType.class );
	}
}
