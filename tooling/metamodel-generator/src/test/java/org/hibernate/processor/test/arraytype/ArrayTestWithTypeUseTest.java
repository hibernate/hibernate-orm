/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.arraytype;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Chris Cranford
 */
public class ArrayTestWithTypeUseTest extends CompilationTest {
	@Test
	@TestForIssue(jiraKey = "HHH-12011")
	@WithClasses(TestEntity.class)
	public void testArrayWithBeanValidation() {
		assertMetamodelClassGeneratedFor(  TestEntity.class );

		// Primitive Arrays
		assertAttributeTypeInMetaModelFor( TestEntity.class, "primitiveAnnotatedArray", byte[].class, "Wrong type for field." );
		assertAttributeTypeInMetaModelFor( TestEntity.class, "primitiveArray", byte[].class, "Wrong type for field." );

		// Primitive non-array
		assertAttributeTypeInMetaModelFor( TestEntity.class, "primitiveAnnotated", Byte.class, "Wrong type for field." );
		assertAttributeTypeInMetaModelFor( TestEntity.class, "primitive", Byte.class, "Wrong type for field." );

		// Non-primitive Arrays
		assertAttributeTypeInMetaModelFor( TestEntity.class, "nonPrimitiveAnnotatedArray", Byte[].class, "Wrong type for field." );
		assertAttributeTypeInMetaModelFor( TestEntity.class, "nonPrimitiveArray", Byte[].class, "Wrong type for field." );

		// Non-primitive non-array
		assertAttributeTypeInMetaModelFor( TestEntity.class, "nonPrimitiveAnnotated", Byte.class, "Wrong type for field." );
		assertAttributeTypeInMetaModelFor( TestEntity.class, "nonPrimitive", Byte.class, "Wrong type for field." );

		// Custom class type array
		assertAttributeTypeInMetaModelFor( TestEntity.class, "customAnnotatedArray", TestEntity.CustomType[].class, "Wrong type for field." );
		assertAttributeTypeInMetaModelFor( TestEntity.class, "customArray", TestEntity.CustomType[].class, "Wrong type for field." );
	}
}
