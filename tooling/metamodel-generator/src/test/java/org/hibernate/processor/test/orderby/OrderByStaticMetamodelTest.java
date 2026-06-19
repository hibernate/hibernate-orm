/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.orderby;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

@CompilationTest
class OrderByStaticMetamodelTest {

	@Test
	@TestForIssue(jiraKey = "HHH-20556")
	@WithClasses(value = {}, sources = {
			"org.hibernate.processor.test.orderby.OrderByParent",
			"org.hibernate.processor.test.orderby.OrderByChild"
	})
	void orderByAcceptsStaticMetamodelConstant() {
		assertMetamodelClassGeneratedFor( "org.hibernate.processor.test.orderby.OrderByParent" );
		assertMetamodelClassGeneratedFor( "org.hibernate.processor.test.orderby.OrderByChild" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-20556")
	@WithClasses(value = {}, sources = {
			"org.hibernate.processor.test.orderby.OrderByDefaultParent",
			"org.hibernate.processor.test.orderby.OrderByChild"
	})
	void orderByAcceptsDefaultValue() {
		assertMetamodelClassGeneratedFor( "org.hibernate.processor.test.orderby.OrderByDefaultParent" );
		assertMetamodelClassGeneratedFor( "org.hibernate.processor.test.orderby.OrderByChild" );
	}
}
