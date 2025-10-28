/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.supresswarnings;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class SuppressWarningsAnnotationNotGeneratedTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-50")
	@WithClasses(TestEntity.class)
	void testSuppressedWarningsAnnotationNotGenerated() {
		assertMetamodelClassGeneratedFor( TestEntity.class );

		// need to check the source because @SuppressWarnings is not a runtime annotation
		String metaModelSource = getMetaModelSourceAsString( TestEntity.class );
		assertFalse(
				metaModelSource.contains( "@SuppressWarnings(\"all\")" ),
				"@SuppressWarnings should not be added to the metamodel."
		);
	}
}
