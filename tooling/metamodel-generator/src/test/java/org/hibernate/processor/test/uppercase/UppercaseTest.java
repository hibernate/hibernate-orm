/*
 * SPDX-License-Identifier: Apache-2.0
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
		assertPresenceOfFieldInMetamodelFor( Person.class, "fullName" );
		assertPresenceOfFieldInMetamodelFor( Person.class, "FULL_NAME" );
		assertPresenceOfFieldInMetamodelFor( Person.class, "X" );
		assertPresenceOfFieldInMetamodelFor( Person.class, "_X" );
		assertPresenceOfFieldInMetamodelFor( Person.class, "y" );
		assertPresenceOfFieldInMetamodelFor( Person.class, "Y" );
	}
}
