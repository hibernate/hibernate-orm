/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CompositeIdDescriptor}.
 *
 * @author Koen Aers
 */
public class CompositeIdDescriptorTest {

	@Test
	public void testConstructorAndDefaults() {
		CompositeIdDescriptor compositeId =
			new CompositeIdDescriptor("id", "OrderItemId", "com.example");

		assertEquals("id", compositeId.getFieldName());
		assertEquals("OrderItemId", compositeId.getIdClassName());
		assertEquals("com.example", compositeId.getIdClassPackage());
		assertNotNull(compositeId.getAttributeOverrides());
		assertTrue(compositeId.getAttributeOverrides().isEmpty());
	}

	@Test
	public void testAddAttributeOverride() {
		CompositeIdDescriptor compositeId =
			new CompositeIdDescriptor("id", "OrderItemId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("productId", "PRODUCT_ID");

		List<AttributeOverrideDescriptor> overrides = compositeId.getAttributeOverrides();
		assertEquals(2, overrides.size());
		assertEquals("orderId", overrides.get(0).getFieldName());
		assertEquals("ORDER_ID", overrides.get(0).getColumnName());
		assertEquals("productId", overrides.get(1).getFieldName());
		assertEquals("PRODUCT_ID", overrides.get(1).getColumnName());
	}
}
