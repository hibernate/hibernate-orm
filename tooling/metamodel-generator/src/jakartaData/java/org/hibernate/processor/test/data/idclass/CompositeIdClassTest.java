/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.idclass;

import org.hibernate.processor.test.data.idclass.MyEntity.MyEntityId;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

public class CompositeIdClassTest extends CompilationTest {
	@Test
	@WithClasses({
			MyRepository.class,
			MyEntity.class,
	})
	public void test() {
		System.out.println( getMetaModelSourceAsString( MyEntity.class ) );
		System.out.println( getMetaModelSourceAsString( MyEntity.class, true ) );
		System.out.println( getMetaModelSourceAsString( MyRepository.class ) );
		assertMetamodelClassGeneratedFor( MyEntity.class );
		assertMetamodelClassGeneratedFor( MyEntity.class, true );
		assertMetamodelClassGeneratedFor( MyRepository.class );
		assertPresenceOfMethodInMetamodelFor(
				MyRepository.class,
				"findById",
				MyEntityId.class
		);
	}
}
