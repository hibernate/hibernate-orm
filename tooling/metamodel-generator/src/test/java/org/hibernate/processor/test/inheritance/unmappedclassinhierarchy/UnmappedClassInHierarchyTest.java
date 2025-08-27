/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.inheritance.unmappedclassinhierarchy;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertSuperclassRelationshipInMetamodel;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@CompilationTest
class UnmappedClassInHierarchyTest {
	@Test
	@WithClasses({
			BaseEntity.class,
			MappedBase.class,
			NormalExtendsEntity.class,
			NormalExtendsMapped.class,
			SubA.class,
			SubB.class
	})
	void testUnmappedClassInHierarchy() throws Exception {
		assertSuperclassRelationshipInMetamodel( SubA.class, BaseEntity.class );
		assertSuperclassRelationshipInMetamodel( SubB.class, MappedBase.class );
	}
}
