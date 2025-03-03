/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mappedsuperclass.embeddablemappedsuperclass;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;

import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
public class EmbeddableMappedSuperClassTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-36")
	@WithClasses(EmbeddableAndMappedSuperClass.class)
	public void testMetaModelsGenerated() {
		assertMetamodelClassGeneratedFor( EmbeddableAndMappedSuperClass.class );
	}
}
