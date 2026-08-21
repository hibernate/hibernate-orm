/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.arraytype;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class ArrayTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-2")
	@WithClasses(Image.class)
	void testPrimitiveArray() {
		assertAttributeTypeInMetaModelFor( Image.class, "data", byte[].class, "Wrong type for field." );
	}

	@Test
	@TestForIssue(jiraKey = "METAGEN-2")
	@WithClasses(TemperatureSamples.class)
	void testIntegerArray() {
		assertAttributeTypeInMetaModelFor(
				TemperatureSamples.class, "samples", Integer[].class, "Wrong type for field."
		);
	}
}
