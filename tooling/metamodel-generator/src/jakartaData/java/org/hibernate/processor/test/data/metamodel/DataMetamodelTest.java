/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.metamodel;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.data.metamodel.Attribute;
import jakarta.data.metamodel.BasicAttribute;
import jakarta.data.metamodel.BooleanAttribute;
import jakarta.data.metamodel.ComparableAttribute;
import jakarta.data.metamodel.NavigableAttribute;
import jakarta.data.metamodel.NumericAttribute;
import jakarta.data.metamodel.TemporalAttribute;
import jakarta.data.metamodel.TextAttribute;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class DataMetamodelTest {
	@Test
	@WithClasses({
			DataMetamodelEntity.class,
			DataMetamodelPart.class,
			DataMetamodelRelated.class,
			DataMetamodelStatus.class
	})
	void generatedDataMetamodelUsesJakartaData11AttributeTypes() throws Exception {
		assertAttribute( "id", NumericAttribute.class, DataMetamodelEntity.class, Long.class );
		assertAttribute( "title", TextAttribute.class, DataMetamodelEntity.class );
		assertAttribute( "published", BooleanAttribute.class, DataMetamodelEntity.class );
		assertAttribute( "pages", NumericAttribute.class, DataMetamodelEntity.class, Integer.class );
		assertAttribute( "price", NumericAttribute.class, DataMetamodelEntity.class, BigDecimal.class );
		assertAttribute( "publicationDate", TemporalAttribute.class, DataMetamodelEntity.class, LocalDate.class );
		assertAttribute( "status", ComparableAttribute.class, DataMetamodelEntity.class, DataMetamodelStatus.class );
		assertAttribute( "payload", BasicAttribute.class, DataMetamodelEntity.class, Object.class );
		assertAttribute( "bytes", BasicAttribute.class, DataMetamodelEntity.class, byte[].class );
		assertAttribute( "part", NavigableAttribute.class, DataMetamodelEntity.class, DataMetamodelPart.class );
		assertAttribute( "related", NavigableAttribute.class, DataMetamodelEntity.class, DataMetamodelRelated.class );
		assertAttribute( "part_width", NumericAttribute.class, DataMetamodelEntity.class, Integer.class );
		assertAttribute( "part_label", TextAttribute.class, DataMetamodelEntity.class );

		final Attribute<?> pages = assertAttributeValue( "pages", "pages", int.class );
		assertEquals( DataMetamodelEntity.class, pages.declaringType() );

		final String source = getMetaModelSourceAsString( DataMetamodelEntity.class, true );
		assertFalse( source.contains( "jakarta.data.metamodel.impl" ) );
		assertTrue( source.contains( "TextAttribute.of(DataMetamodelEntity.class, TITLE)" ) );
		assertTrue( source.contains( "NumericAttribute.of(DataMetamodelEntity.class, PAGES, int.class)" ) );
	}

	private static void assertAttribute(String fieldName, Class<?> expectedRawType, Type... expectedTypeArguments)
			throws NoSuchFieldException {
		final var field = dataMetamodelField( fieldName );
		final var parameterizedType = assertInstanceOf( ParameterizedType.class, field.getGenericType() );
		assertEquals( expectedRawType, parameterizedType.getRawType() );
		assertArrayEquals( expectedTypeArguments, parameterizedType.getActualTypeArguments() );
	}

	private static Attribute<?> assertAttributeValue(String fieldName, String expectedName, Class<?> expectedType)
			throws ReflectiveOperationException {
		final var attribute = assertInstanceOf( Attribute.class, dataMetamodelField( fieldName ).get( null ) );
		assertEquals( expectedName, attribute.name() );
		assertEquals( expectedType, attribute.type() );
		return attribute;
	}

	private static Field dataMetamodelField(String fieldName) throws NoSuchFieldException {
		return getMetamodelClassFor( DataMetamodelEntity.class, true ).getDeclaredField( fieldName );
	}
}
