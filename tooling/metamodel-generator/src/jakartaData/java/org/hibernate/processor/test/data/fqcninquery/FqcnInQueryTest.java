/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.fqcninquery;

import jakarta.persistence.EntityManager;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

@CompilationTest
class FqcnInQueryTest {
	@Test
	@WithClasses({MyEntity.class, MyRepository.class})
	void test() {
		System.out.println( getMetaModelSourceAsString( MyRepository.class ) );
		System.out.println( getMetaModelSourceAsString( MyEntity.class ) );

		assertMetamodelClassGeneratedFor( MyRepository.class );
		assertMetamodelClassGeneratedFor( MyEntity.class );

		assertPresenceOfMethodInMetamodelFor( MyEntity.class, "getName", EntityManager.class, Integer.class );
		assertPresenceOfMethodInMetamodelFor( MyEntity.class, "getUniqueId", EntityManager.class, String.class );

		assertPresenceOfMethodInMetamodelFor( MyRepository.class, "getName", Integer.class );
		assertPresenceOfMethodInMetamodelFor( MyRepository.class, "getUniqueId", String.class );
	}
}
