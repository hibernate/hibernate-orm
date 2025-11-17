/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.namedquery;

import jakarta.persistence.EntityManager;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

@CompilationTest
class ThingTest {
	@Test @WithClasses( Thing.class )
	void test() {
		System.out.println( getMetaModelSourceAsString( Thing.class) );
		System.out.println( getMetaModelSourceAsString( Thing.class, true) );
		assertMetamodelClassGeneratedFor(Thing.class);
		assertMetamodelClassGeneratedFor(Thing.class, true);
		assertPresenceOfMethodInMetamodelFor( Thing.class, "things", EntityManager.class, String.class );
		assertPresenceOfFieldInMetamodelFor( Thing.class, "__things_" );
	}
}
