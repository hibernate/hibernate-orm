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
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.Assert.assertFalse;

/**
 * @author Hardy Ferentschik
 */
public class SkipGeneratedAnnotationTest extends CompilationTest {
	@Test
	@TestForIssue(jiraKey = "METAGEN-79")
	@WithClasses(TestEntity.class)
	@WithProcessorOption(key = HibernateProcessor.ADD_GENERATED_ANNOTATION, value = "false")
	public void testGeneratedAnnotationGenerated() {
		assertMetamodelClassGeneratedFor( TestEntity.class );

		// need to check the source because @Generated is not a runtime annotation
		String metaModelSource = getMetaModelSourceAsString( TestEntity.class );
		assertFalse( "@Generated should not be added to the metamodel.", metaModelSource.contains( "@Generated" ) );
	}
}
