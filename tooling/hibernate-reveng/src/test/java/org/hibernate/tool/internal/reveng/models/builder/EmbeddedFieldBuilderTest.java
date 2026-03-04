/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.builder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddedFieldMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Embedded;

/**
 * Tests for {@link EmbeddedFieldBuilder}, verifying that {@code @Embedded}
 * and {@code @AttributeOverrides} annotations are correctly applied to fields.
 *
 * @author Koen Aers
 */
public class EmbeddedFieldBuilderTest {

	private ModelsContext modelsContext;
	private DynamicClassDetails entityClass;
	private ClassDetails embeddableClassDetails;

	@BeforeEach
	public void setUp() {
		ClassLoading classLoading = SimpleClassLoading.SIMPLE_CLASS_LOADING;
		modelsContext = new BasicModelsContextImpl(classLoading, false, null);
		entityClass = new DynamicClassDetails(
			"Customer",
			"com.example.Customer",
			false,
			null,
			null,
			modelsContext
		);
		embeddableClassDetails = new DynamicClassDetails(
			"Address",
			"com.example.Address",
			false,
			null,
			null,
			modelsContext
		);
	}

	@Test
	public void testEmbeddedAnnotation() {
		EmbeddedFieldMetadata embeddedMetadata =
			new EmbeddedFieldMetadata("address", "Address", "com.example");

		EmbeddedFieldBuilder.buildEmbeddedField(
			entityClass, embeddedMetadata, embeddableClassDetails, modelsContext);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("address", field.getName());

		Embedded embedded = field.getAnnotationUsage(Embedded.class, modelsContext);
		assertNotNull(embedded, "Should have @Embedded");
	}

	@Test
	public void testFieldType() {
		EmbeddedFieldMetadata embeddedMetadata =
			new EmbeddedFieldMetadata("address", "Address", "com.example");

		EmbeddedFieldBuilder.buildEmbeddedField(
			entityClass, embeddedMetadata, embeddableClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("Address", field.getType().getName());
	}

	@Test
	public void testNoAttributeOverrides() {
		EmbeddedFieldMetadata embeddedMetadata =
			new EmbeddedFieldMetadata("address", "Address", "com.example");

		EmbeddedFieldBuilder.buildEmbeddedField(
			entityClass, embeddedMetadata, embeddableClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNull(field.getAnnotationUsage(AttributeOverrides.class, modelsContext),
			"Should NOT have @AttributeOverrides when no overrides are configured");
	}

	@Test
	public void testWithAttributeOverrides() {
		EmbeddedFieldMetadata embeddedMetadata =
			new EmbeddedFieldMetadata("address", "Address", "com.example")
				.addAttributeOverride("street", "STREET_NAME")
				.addAttributeOverride("city", "CITY_NAME");

		EmbeddedFieldBuilder.buildEmbeddedField(
			entityClass, embeddedMetadata, embeddableClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNotNull(field.getAnnotationUsage(Embedded.class, modelsContext),
			"Should have @Embedded");

		AttributeOverrides overrides =
			field.getAnnotationUsage(AttributeOverrides.class, modelsContext);
		assertNotNull(overrides, "Should have @AttributeOverrides");

		AttributeOverride[] values = overrides.value();
		assertEquals(2, values.length);

		assertEquals("street", values[0].name());
		assertEquals("STREET_NAME", values[0].column().name());

		assertEquals("city", values[1].name());
		assertEquals("CITY_NAME", values[1].column().name());
	}

	@Test
	public void testSingleAttributeOverride() {
		EmbeddedFieldMetadata embeddedMetadata =
			new EmbeddedFieldMetadata("address", "Address", "com.example")
				.addAttributeOverride("zipCode", "ZIP_CODE");

		EmbeddedFieldBuilder.buildEmbeddedField(
			entityClass, embeddedMetadata, embeddableClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		AttributeOverrides overrides =
			field.getAnnotationUsage(AttributeOverrides.class, modelsContext);
		assertNotNull(overrides);

		AttributeOverride[] values = overrides.value();
		assertEquals(1, values.length);
		assertEquals("zipCode", values[0].name());
		assertEquals("ZIP_CODE", values[0].column().name());
	}
}
