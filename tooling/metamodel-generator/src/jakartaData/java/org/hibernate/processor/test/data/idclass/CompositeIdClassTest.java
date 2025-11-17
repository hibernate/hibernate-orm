/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.idclass;

import org.hibernate.processor.test.data.idclass.MyEntity.MyEntityId;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

@CompilationTest
class CompositeIdClassTest {
	@Test
	@WithClasses({
			MyRepository.class,
			YourRepository.class,
			MyEntity.class,
	})
	void test() {
		System.out.println( getMetaModelSourceAsString( MyEntity.class ) );
		System.out.println( getMetaModelSourceAsString( MyEntity.class, true ) );
		System.out.println( getMetaModelSourceAsString( MyRepository.class ) );
		System.out.println( getMetaModelSourceAsString( YourRepository.class ) );
		assertMetamodelClassGeneratedFor( MyEntity.class );
		assertMetamodelClassGeneratedFor( MyEntity.class, true );
		assertMetamodelClassGeneratedFor( MyRepository.class );
		assertMetamodelClassGeneratedFor( YourRepository.class );
		assertPresenceOfMethodInMetamodelFor(
				MyRepository.class,
				"findById",
				MyEntityId.class
		);
	}
}
