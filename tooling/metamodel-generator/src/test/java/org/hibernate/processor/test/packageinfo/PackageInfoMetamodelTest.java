/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.packageinfo;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

@CompilationTest
class PackageInfoMetamodelTest {

	@Test
	@WithClasses(value = {}, sources = {
			"org.hibernate.processor.test.packageinfo.Message",
			"org.hibernate.processor.test.packageinfo.package-info"
	})
	void test() {
		assertMetamodelClassGeneratedFor( "org.hibernate.processor.test.packageinfo.Message" );

		System.out.println( getMetaModelSourceAsString( "org.hibernate.processor.test.packageinfo.packageinfo" ) );

		assertPresenceOfFieldInMetamodelFor(
				"org.hibernate.processor.test.packageinfo.packageinfo",
				"QUERY_FIND_BY_KEY"
		);
		assertPresenceOfFieldInMetamodelFor(
				"org.hibernate.processor.test.packageinfo.packageinfo",
				"QUERY_FIND_BY_ID_AND_KEY"
		);

		assertPresenceOfMethodInMetamodelFor(
				"org.hibernate.processor.test.packageinfo.packageinfo",
				"findByKey",
				EntityManager.class,
				String.class
		);
	}
}
