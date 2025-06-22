/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.supresswarnings;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class SuppressExplicitWarningsAnnotationGeneratedTest extends CompilationTest {
	@Test
	@WithClasses(TestEntity.class)
	@WithProcessorOption(key = HibernateProcessor.ADD_SUPPRESS_WARNINGS_ANNOTATION, value = "foo, bar")
	public void testSuppressedWarningsAnnotationGenerated() {
		assertMetamodelClassGeneratedFor( TestEntity.class );

		// need to check the source because @SuppressWarnings is not a runtime annotation
		String metaModelSource = getMetaModelSourceAsString( TestEntity.class );
		assertTrue(
				"@SuppressWarnings should be added to the metamodel.",
				metaModelSource.contains( "@SuppressWarnings({\"foo\", \"bar\"})" )
		);
	}
}
