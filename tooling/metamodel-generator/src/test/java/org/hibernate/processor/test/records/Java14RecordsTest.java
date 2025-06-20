/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.records;

import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static java.lang.reflect.Modifier.isStatic;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getFieldFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@CompilationTest
class Java14RecordsTest {

	@Test
	@TestForIssue(jiraKey = "HHH-16261")
	@WithClasses({Address.class, Author.class})
	void testEmbeddableRecordProperty() {
		assertMetamodelClassGeneratedFor(Address.class);
		for (final String fieldName : List.of("street", "city", "postalCode")) {
			assertNotNull(getFieldFromMetamodelFor(Address.class, fieldName),"Address must contain '" + fieldName + "' field");
		}
		assertMetamodelClassGeneratedFor(Author.class);

		final Field addressField = getFieldFromMetamodelFor(Author.class, "address");
		assertNotNull(addressField, "Author must contain 'address' field");
		assertTrue(isStatic(addressField.getModifiers()));
		if (addressField.getGenericType() instanceof ParameterizedType parameterizedType) {
			assertEquals(SingularAttribute.class, parameterizedType.getRawType());
			final Type[] typeArguments = parameterizedType.getActualTypeArguments();
			assertEquals(2, typeArguments.length);
			assertEquals(Author.class, typeArguments[0]);
			assertEquals(Address.class, typeArguments[1]);
		} else {
			fail("Address field must be instance of ParameterizedType");
		}

		final Field addressNameField = getFieldFromMetamodelFor(Author.class, "address".toUpperCase());
		assertNotNull(addressNameField, "Author must contain 'ADDRESS' field");
		assertTrue(isStatic(addressNameField.getModifiers()));
		assertEquals(String.class, addressNameField.getGenericType());
	}
}
