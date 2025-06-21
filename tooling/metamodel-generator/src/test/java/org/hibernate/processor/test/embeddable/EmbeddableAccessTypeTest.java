/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class EmbeddableAccessTypeTest {
	@Test
	@WithClasses({ Base.class, EmbeddableEntity.class, IStuff.class, MyEntity.class, Stuff.class })
	void testCorrectAccessTypeUsedForEmbeddable() {
		assertAttributeTypeInMetaModelFor(
				EmbeddableEntity.class,
				"stuffs",
				Stuff.class,
				"The target annotation set the type to Stuff"
		);
	}
}
