/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.constant;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;

@CompilationTest
@TestForIssue(jiraKey = "HHH-18106")
class ConstantInNamedQueryTest {

	@Test
	@WithClasses(value = {}, sources = "org.hibernate.processor.test.constant.CookBookWithCheck")
	void withCheckHQL() {
		final String entityClass = "org.hibernate.processor.test.constant.CookBookWithCheck";

		System.out.println( TestUtil.getMetaModelSourceAsString( entityClass ) );

		assertMetamodelClassGeneratedFor( entityClass );

		assertPresenceOfFieldInMetamodelFor( entityClass, "QUERY_FIND_BAD_BOOKS" );
		assertPresenceOfFieldInMetamodelFor( entityClass, "QUERY_FIND_GOOD_BOOKS" );

		assertPresenceOfMethodInMetamodelFor( entityClass, "findBadBooks", EntityManager.class );
		assertPresenceOfMethodInMetamodelFor( entityClass, "findGoodBooks", EntityManager.class );
	}

	@Test
	@WithClasses(value = CookBookWithoutCheck.class, sources = "org.hibernate.processor.test.constant.NumericBookType")
	void withoutCheckHQL() {
		final String entityClass = "org.hibernate.processor.test.constant.CookBookWithoutCheck";

		System.out.println( TestUtil.getMetaModelSourceAsString( entityClass ) );

		assertMetamodelClassGeneratedFor( entityClass );
		assertPresenceOfFieldInMetamodelFor( entityClass, "QUERY_FIND_GOOD_BOOKS" );

		assertPresenceOfMethodInMetamodelFor( entityClass, "findGoodBooks", EntityManager.class );
	}
}
