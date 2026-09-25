/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.wildcard;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * Verifies that the annotation processor does not crash with
 * {@code IllegalArgumentException} when a {@code @Repository} interface
 * extends a type hierarchy containing wildcard types like
 * {@code Class<? extends E>} and uses {@code @jakarta.data.repository.Query}.
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-20941">HHH-20941</a>
 */
@CompilationTest
class WildcardQueryTest {
	@Test
	@WithClasses({ MyEntity.class, ManagedOperations.class, WildcardRepository.class })
	void testWildcardTypeInHierarchyWithQuery() {
		System.out.println( TestUtil.getMetaModelSourceAsString( WildcardRepository.class, true ) );
		assertMetamodelClassGeneratedFor( MyEntity.class );
		assertMetamodelClassGeneratedFor( WildcardRepository.class, true );
	}
}
