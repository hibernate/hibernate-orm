/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12612")
public class EmbeddableTypeUseTest extends CompilationTest {
	@Test
	@WithClasses({SimpleEntity.class})
	public void testAnnotatedEmbeddable() {
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
