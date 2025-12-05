/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.generic;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

@CompilationTest
public class MyMappedSuperclassTest {

	@Test
	@WithClasses({MyActualEntity.class, MyActualEntityRepository.class, MyMappedSuperclass.class})
	void smoke() {
		System.out.println( TestUtil.getMetaModelSourceAsString( MyMappedSuperclass.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( MyActualEntity.class ) );
		assertMetamodelClassGeneratedFor( MyMappedSuperclass.class );
		assertMetamodelClassGeneratedFor( MyActualEntity.class );
	}
}
