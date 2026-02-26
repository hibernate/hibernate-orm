/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.hhh20212;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

@CompilationTest
class HHH20212Test {
	@Test
	@WithClasses({ HHH20212Entity.class, HHH20212Repository.class })
	void test() {
		System.out.println( getMetaModelSourceAsString( HHH20212Repository.class ) );
		assertMetamodelClassGeneratedFor( HHH20212Repository.class );
	}
}
