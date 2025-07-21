/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.constraint;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

@CompilationTest
class DataTest {
	@Test
	@WithClasses({MyEntity.class, MyConstrainedRepository.class})
	void test() {
		System.out.println( getMetaModelSourceAsString( MyEntity.class ) );
		System.out.println( getMetaModelSourceAsString( MyConstrainedRepository.class ) );
		assertMetamodelClassGeneratedFor( MyEntity.class );
//		assertMetamodelClassGeneratedFor( MyConstrainedRepository.class );
	}
}
