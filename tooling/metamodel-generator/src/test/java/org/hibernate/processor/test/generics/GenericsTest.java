/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.generics;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Emmanuel Bernard
 */
@CompilationTest
class GenericsTest {

	@Test
	@WithClasses({ Parent.class, Child.class })
	void testGenerics() {
		assertMetamodelClassGeneratedFor( Parent.class );
		assertMetamodelClassGeneratedFor( Child.class );
	}
}
