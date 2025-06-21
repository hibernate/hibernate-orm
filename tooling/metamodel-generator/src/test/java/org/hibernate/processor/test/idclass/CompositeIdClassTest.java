/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.idclass;

import jakarta.persistence.EntityManager;
import org.hibernate.processor.test.idclass.MyEntity.MyEntityId;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfNameFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

@CompilationTest
class CompositeIdClassTest {
	@Test
	@WithClasses(MyEntity.class)
	void test() {
		System.out.println( getMetaModelSourceAsString( MyEntity.class ) );
		assertMetamodelClassGeneratedFor( MyEntity.class );
		assertPresenceOfNameFieldInMetamodelFor(
				MyEntity.class,
				"QUERY_FIND_BY_ID",
				"Missing named query attribute."
		);
		assertPresenceOfMethodInMetamodelFor(
				MyEntity.class,
				"findById",
				EntityManager.class,
				MyEntityId.class
		);
	}
}
