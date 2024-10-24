/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.namedquery;

import jakarta.persistence.EntityManager;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

public class ThingTest extends CompilationTest {
	@Test @WithClasses( Thing.class )
	public void test() {
		System.out.println( getMetaModelSourceAsString( Thing.class) );
		System.out.println( getMetaModelSourceAsString( Thing.class, true) );
		assertMetamodelClassGeneratedFor(Thing.class);
		assertMetamodelClassGeneratedFor(Thing.class, true);
		assertPresenceOfMethodInMetamodelFor( Thing.class, "things", EntityManager.class, String.class );
	}
}
