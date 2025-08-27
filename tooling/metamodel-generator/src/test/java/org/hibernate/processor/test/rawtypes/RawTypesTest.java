/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.rawtypes;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Emmanuel Bernard
 */
@CompilationTest
class RawTypesTest {

	@Test
	@WithClasses({ DeskWithRawType.class, EmployeeWithRawType.class })
	void testGenerics() {
		assertMetamodelClassGeneratedFor( DeskWithRawType.class );
		assertMetamodelClassGeneratedFor( EmployeeWithRawType.class );
	}
}
