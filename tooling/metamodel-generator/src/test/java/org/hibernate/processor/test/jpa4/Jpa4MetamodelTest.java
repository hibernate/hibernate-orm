/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.jpa4;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.metamodel.BooleanAttribute;
import jakarta.persistence.metamodel.ComparableAttribute;
import jakarta.persistence.metamodel.NumericAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.TemporalAttribute;
import jakarta.persistence.metamodel.TextAttribute;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getFieldFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@CompilationTest
class Jpa4MetamodelTest {
	@Test
	@TestForIssue(jiraKey = "HHH-20363")
	@WithClasses({ Book.class, Status.class })
	void generatedMetamodelUsesSpecializedSingularAttributes() {
		assertAttribute( "id", NumericAttribute.class, Book.class, Long.class );
		assertAttribute( "title", TextAttribute.class, Book.class );
		assertAttribute( "published", BooleanAttribute.class, Book.class );
		assertAttribute( "pages", NumericAttribute.class, Book.class, Integer.class );
		assertAttribute( "price", NumericAttribute.class, Book.class, BigDecimal.class );
		assertAttribute( "publicationDate", TemporalAttribute.class, Book.class, LocalDate.class );
		assertAttribute( "status", ComparableAttribute.class, Book.class, Status.class );
		assertAttribute( "payload", SingularAttribute.class, Book.class, Object.class );
	}

	private static void assertAttribute(String fieldName, Class<?> expectedRawType, Type... expectedTypeArguments) {
		final var field = getFieldFromMetamodelFor( Book.class, fieldName );
		final var parameterizedType = assertInstanceOf( ParameterizedType.class, field.getGenericType() );
		assertEquals( expectedRawType, parameterizedType.getRawType() );
		assertArrayEquals( expectedTypeArguments, parameterizedType.getActualTypeArguments() );
	}
}
