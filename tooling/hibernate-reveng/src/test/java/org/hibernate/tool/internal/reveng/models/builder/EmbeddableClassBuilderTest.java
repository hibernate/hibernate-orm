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
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddableMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Tests for {@link EmbeddableClassBuilder}, verifying that {@code @Embeddable}
 * classes are correctly built with fields and registered in the models context.
 *
 * @author Koen Aers
 */
public class EmbeddableClassBuilderTest {

	private ModelsContext modelsContext;

	@BeforeEach
	public void setUp() {
		ClassLoading classLoading = SimpleClassLoading.SIMPLE_CLASS_LOADING;
		modelsContext = new BasicModelsContextImpl(classLoading, false, null);
	}

	@Test
	public void testEmbeddableAnnotation() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("Address", "com.example");

		ClassDetails result = EmbeddableClassBuilder.buildEmbeddableClass(metadata, modelsContext);

		Embeddable embeddable = result.getAnnotationUsage(Embeddable.class, modelsContext);
		assertNotNull(embeddable, "Should have @Embeddable");
	}

	@Test
	public void testClassName() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("Address", "com.example");

		ClassDetails result = EmbeddableClassBuilder.buildEmbeddableClass(metadata, modelsContext);

		assertEquals("Address", result.getName());
	}

	@Test
	public void testNoFields() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("EmptyId", "com.example");

		ClassDetails result = EmbeddableClassBuilder.buildEmbeddableClass(metadata, modelsContext);

		assertNotNull(result.getAnnotationUsage(Embeddable.class, modelsContext));
		assertTrue(result.getFields().isEmpty(), "Should have no fields");
	}

	@Test
	public void testWithFields() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("Address", "com.example")
			.addColumn(new ColumnMetadata("STREET", "street", String.class))
			.addColumn(new ColumnMetadata("CITY", "city", String.class))
			.addColumn(new ColumnMetadata("ZIP_CODE", "zipCode", String.class));

		ClassDetails result = EmbeddableClassBuilder.buildEmbeddableClass(metadata, modelsContext);

		List<FieldDetails> fields = result.getFields();
		assertEquals(3, fields.size());

		assertEquals("street", fields.get(0).getName());
		assertEquals("city", fields.get(1).getName());
		assertEquals("zipCode", fields.get(2).getName());
	}

	@Test
	public void testFieldColumnAnnotations() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("Address", "com.example")
			.addColumn(new ColumnMetadata("STREET_NAME", "street", String.class).length(200))
			.addColumn(new ColumnMetadata("CITY", "city", String.class).nullable(false));

		ClassDetails result = EmbeddableClassBuilder.buildEmbeddableClass(metadata, modelsContext);

		List<FieldDetails> fields = result.getFields();
		assertEquals(2, fields.size());

		Column streetCol = fields.get(0).getAnnotationUsage(Column.class, modelsContext);
		assertNotNull(streetCol);
		assertEquals("STREET_NAME", streetCol.name());
		assertEquals(200, streetCol.length());

		Column cityCol = fields.get(1).getAnnotationUsage(Column.class, modelsContext);
		assertNotNull(cityCol);
		assertEquals("CITY", cityCol.name());
		assertFalse(cityCol.nullable());
	}

	@Test
	public void testRegisteredInContext() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("Address", "com.example");

		EmbeddableClassBuilder.buildEmbeddableClass(metadata, modelsContext);

		ClassDetails resolved = modelsContext.getClassDetailsRegistry()
			.resolveClassDetails("com.example.Address");
		assertNotNull(resolved, "Should be registered in the class details registry");
		assertEquals("Address", resolved.getName());
	}
}
