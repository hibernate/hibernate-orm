/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.supresswarnings;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class SuppressWarningsAnnotationGeneratedTest extends CompilationTest {
	@Test
	@TestForIssue(jiraKey = "METAGEN-50")
	@WithClasses(TestEntity.class)
	@WithProcessorOption(key = HibernateProcessor.ADD_SUPPRESS_WARNINGS_ANNOTATION, value = "true")
	public void testSuppressedWarningsAnnotationGenerated() {
		assertMetamodelClassGeneratedFor( TestEntity.class );

		// need to check the source because @SuppressWarnings is not a runtime annotation
		String metaModelSource = getMetaModelSourceAsString( TestEntity.class );
		assertTrue(
				"@SuppressWarnings should be added to the metamodel.",
				metaModelSource.contains( "@SuppressWarnings({\"deprecation\", \"rawtypes\"})" )
		);
	}
}
