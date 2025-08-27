/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Chris Cranford
 */
@CompilationTest
@TestForIssue(jiraKey = "HHH-12612")
class EmbeddableTypeUseTest {
	@Test
	@WithClasses({SimpleEntity.class})
	void testAnnotatedEmbeddable() {
		System.out.println( TestUtil.getMetaModelSourceAsString( SimpleEntity.class ) );
		assertMetamodelClassGeneratedFor( SimpleEntity.class );
		assertAttributeTypeInMetaModelFor(
				SimpleEntity.class,
				"simpleEmbeddable",
				SimpleEmbeddable.class,
				"Wrong type for embeddable attribute."
		);
	}
}
