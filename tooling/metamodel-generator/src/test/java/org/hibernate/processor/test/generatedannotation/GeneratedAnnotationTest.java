/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.generatedannotation;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class GeneratedAnnotationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-79")
	@WithClasses(TestEntity.class)
	void testGeneratedAnnotationNotGenerated() {
		assertMetamodelClassGeneratedFor( TestEntity.class );

		// need to check the source because @Generated is not a runtime annotation
		String metaModelSource = getMetaModelSourceAsString( TestEntity.class );
		String generatedString = "@Generated(\"org.hibernate.processor.HibernateProcessor\")";
		assertTrue( metaModelSource.contains( generatedString ), "@Generated should be added to the metamodel." );
	}
}
