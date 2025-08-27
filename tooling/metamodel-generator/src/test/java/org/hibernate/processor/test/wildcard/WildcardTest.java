/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.wildcard;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Gavin King
 */
@CompilationTest
class WildcardTest {
	@Test
	@WithClasses({ PropertyRepo.class })
	void testGeneratedAnnotationNotGenerated() {
		System.out.println( TestUtil.getMetaModelSourceAsString( PropertyRepo.class ) );
		assertMetamodelClassGeneratedFor( PropertyRepo.class );
	}
}
