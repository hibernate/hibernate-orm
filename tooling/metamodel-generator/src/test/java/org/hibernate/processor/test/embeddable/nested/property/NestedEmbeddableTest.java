/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable.nested.property;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;

public class NestedEmbeddableTest extends CompilationTest {
	@Test
	@WithClasses({ Author.class, Address.class, Postcode.class })
	public void testCorrectAccessTypeUsedForEmbeddable() {
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
