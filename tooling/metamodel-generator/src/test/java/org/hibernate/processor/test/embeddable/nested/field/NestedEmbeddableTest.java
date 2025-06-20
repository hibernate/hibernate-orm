/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable.nested.field;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;

@CompilationTest
class NestedEmbeddableTest {
	@Test
	@WithClasses({ Author.class, Address.class, Postcode.class })
	void testCorrectAccessTypeUsedForEmbeddable() {
		assertAttributeTypeInMetaModelFor(
				Address.class,
				"city",
				String.class,
				"city should be String"
		);
		assertAttributeTypeInMetaModelFor(
				Postcode.class,
				"zip",
				String.class,
				"zip should be String"
		);
	}
}
