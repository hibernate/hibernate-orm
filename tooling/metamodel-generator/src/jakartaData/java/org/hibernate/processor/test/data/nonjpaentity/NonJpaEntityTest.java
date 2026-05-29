/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.nonjpaentity;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertNoMetamodelClassGeneratedFor;

/**
 * Verifies that the processor gracefully skips a repository whose
 * primary entity uses a non-JPA annotation (e.g. a NoSQL entity),
 * including when a companion interface provides @Query overrides.
 */
@CompilationTest
class NonJpaEntityTest {

	@Test
	@WithClasses({ NoSqlEntity.class, Product.class, ProductRepository.class, ProductRepository$.class })
	void testRepositoryWithNonJpaEntityIsSkipped() {
		assertNoMetamodelClassGeneratedFor( ProductRepository.class );
		assertNoMetamodelClassGeneratedFor( ProductRepository$.class );
	}
}
