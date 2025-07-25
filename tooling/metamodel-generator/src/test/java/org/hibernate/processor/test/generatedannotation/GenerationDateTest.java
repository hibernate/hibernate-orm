/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.generatedannotation;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.dumpMetaModelSourceFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class GenerationDateTest {
	@Test
	@TestForIssue(jiraKey = "METAGEN-73")
	@WithClasses(TestEntity.class)
	@WithProcessorOption(key = HibernateProcessor.ADD_GENERATION_DATE, value = "true")
	void testGeneratedAnnotationGenerated() {
		assertMetamodelClassGeneratedFor( TestEntity.class );

		// need to check the source because @Generated is not a runtime annotation
		String metaModelSource = getMetaModelSourceAsString( TestEntity.class );

		dumpMetaModelSourceFor( TestEntity.class );
		String generatedString = "@Generated(value = \"org.hibernate.processor.HibernateProcessor\", date = \"";

		assertTrue( metaModelSource.contains( generatedString ), "@Generated should also contain the date parameter." );
	}
}
