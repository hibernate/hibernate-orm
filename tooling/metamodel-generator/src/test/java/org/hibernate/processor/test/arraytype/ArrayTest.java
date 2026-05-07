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
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	@TestForIssue(jiraKey = "HHH-20386")
	@WithClasses(JdbcTypeCodeArrayEntity.class)
	void testJdbcTypeCodeArrayPreservesGenericTypeWithAnnotation() {
		// SingularAttribute<JdbcTypeCodeArrayEntity, Set<String>> expected — NOT raw Set
		String source = getMetaModelSourceAsString( JdbcTypeCodeArrayEntity.class );
		assertTrue(
				source.contains( "SingularAttribute<JdbcTypeCodeArrayEntity, Set<String>>" ),
				"Expected SingularAttribute with generic type Set<String> for @JdbcTypeCode(ARRAY) field, but got:\n" + source
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-20386")
	@WithClasses(JdbcTypeCodeArrayEntity.class)
	void testSetFieldWithoutJdbcTypeCodeAnnotationPreservesGenericType() {
		// Same fix must apply to Set<String> fields without @JdbcTypeCode as well
		String source = getMetaModelSourceAsString( JdbcTypeCodeArrayEntity.class );
		long count = source.lines()
				.filter( line -> line.contains( "SingularAttribute<JdbcTypeCodeArrayEntity, Set<String>>" ) )
				.count();
		assertEquals(
				2,
				count,
				"Expected at least 2 fields with SingularAttribute<JdbcTypeCodeArrayEntity, Set<String>>, but got:\n" + source
		);
	}
}
