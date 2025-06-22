/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.classnamecollision;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassNameCollisionTest extends CompilationTest {

	@Test
	@WithClasses({
			Something.class,
			org.hibernate.processor.test.classnamecollision.somewhere.Something.class
	})
	public void testAmbiguousSimpleName() {
		System.out.println( getMetaModelSourceAsString( Something.class ) );
		assertMetamodelClassGeneratedFor( Something.class );
		System.out.println( getMetaModelSourceAsString( org.hibernate.processor.test.classnamecollision.somewhere.Something.class ) );
		assertMetamodelClassGeneratedFor( org.hibernate.processor.test.classnamecollision.somewhere.Something.class );
		assertEquals(
				getMetamodelClassFor( org.hibernate.processor.test.classnamecollision.somewhere.Something.class ).getName(),
				getMetamodelClassFor( Something.class ).getSuperclass()
						.getName() );
	}

}
