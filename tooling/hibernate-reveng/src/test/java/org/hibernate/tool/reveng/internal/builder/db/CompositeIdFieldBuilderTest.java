/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.builder.db;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.reveng.internal.descriptor.CompositeIdDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.EmbeddedId;

/**
 * Tests for {@link CompositeIdFieldBuilder}, verifying that {@code @EmbeddedId}
 * and {@code @AttributeOverrides} annotations are correctly applied to fields.
 *
 * @author Koen Aers
 */
public class CompositeIdFieldBuilderTest {

	private ModelsContext modelsContext;
	private DynamicClassDetails entityClass;
	private ClassDetails idClassDetails;

	@BeforeEach
	public void setUp() {
		ClassLoading classLoading = SimpleClassLoading.SIMPLE_CLASS_LOADING;
		modelsContext = new BasicModelsContextImpl(classLoading, false, null);
		entityClass = new DynamicClassDetails(
			"OrderItem",
			"com.example.OrderItem",
			false,
			null,
			null,
			modelsContext
		);
		idClassDetails = new DynamicClassDetails(
			"OrderItemId",
			"com.example.OrderItemId",
			false,
			null,
			null,
			modelsContext
		);
	}

	@Test
	public void testEmbeddedIdAnnotation() {
		CompositeIdDescriptor compositeId =
			new CompositeIdDescriptor("id", "OrderItemId", "com.example");

		CompositeIdFieldBuilder.buildCompositeIdField(
			entityClass, compositeId, idClassDetails, modelsContext);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("id", field.getName());

		EmbeddedId embeddedId = field.getAnnotationUsage(EmbeddedId.class, modelsContext);
		assertNotNull(embeddedId, "Should have @EmbeddedId");
	}

	@Test
	public void testFieldType() {
		CompositeIdDescriptor compositeId =
			new CompositeIdDescriptor("id", "OrderItemId", "com.example");

		CompositeIdFieldBuilder.buildCompositeIdField(
			entityClass, compositeId, idClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("OrderItemId", field.getType().getName());
	}

	@Test
	public void testNoAttributeOverrides() {
		CompositeIdDescriptor compositeId =
			new CompositeIdDescriptor("id", "OrderItemId", "com.example");

		CompositeIdFieldBuilder.buildCompositeIdField(
			entityClass, compositeId, idClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNull(field.getAnnotationUsage(AttributeOverrides.class, modelsContext),
			"Should NOT have @AttributeOverrides when no overrides are configured");
	}

	@Test
	public void testWithAttributeOverrides() {
		CompositeIdDescriptor compositeId =
			new CompositeIdDescriptor("id", "OrderItemId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("productId", "PRODUCT_ID");

		CompositeIdFieldBuilder.buildCompositeIdField(
			entityClass, compositeId, idClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNotNull(field.getAnnotationUsage(EmbeddedId.class, modelsContext),
			"Should have @EmbeddedId");

		AttributeOverrides overrides =
			field.getAnnotationUsage(AttributeOverrides.class, modelsContext);
		assertNotNull(overrides, "Should have @AttributeOverrides");

		AttributeOverride[] values = overrides.value();
		assertEquals(2, values.length);

		assertEquals("orderId", values[0].name());
		assertEquals("ORDER_ID", values[0].column().name());

		assertEquals("productId", values[1].name());
		assertEquals("PRODUCT_ID", values[1].column().name());
	}

	@Test
	public void testSingleAttributeOverride() {
		CompositeIdDescriptor compositeId =
			new CompositeIdDescriptor("id", "OrderItemId", "com.example")
				.addAttributeOverride("key", "PK_COLUMN");

		CompositeIdFieldBuilder.buildCompositeIdField(
			entityClass, compositeId, idClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		AttributeOverrides overrides =
			field.getAnnotationUsage(AttributeOverrides.class, modelsContext);
		assertNotNull(overrides);

		AttributeOverride[] values = overrides.value();
		assertEquals(1, values.length);
		assertEquals("key", values[0].name());
		assertEquals("PK_COLUMN", values[0].column().name());
	}
}
