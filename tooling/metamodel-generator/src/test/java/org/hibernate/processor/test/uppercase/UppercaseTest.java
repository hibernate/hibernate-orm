/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.uppercase;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

public class UppercaseTest extends CompilationTest {

	@Test
	@WithClasses(value = Person.class)
	public void test() {
		System.out.println( getMetaModelSourceAsString( Person.class ) );

		assertMetamodelClassGeneratedFor( Person.class );

		assertPresenceOfFieldInMetamodelFor( Person.class, "SSN" );
		assertPresenceOfFieldInMetamodelFor( Person.class, "_SSN" );
		assertPresenceOfFieldInMetamodelFor( Person.class, "UserID" );
		assertPresenceOfFieldInMetamodelFor( Person.class, "USER_ID" );
	}
}
