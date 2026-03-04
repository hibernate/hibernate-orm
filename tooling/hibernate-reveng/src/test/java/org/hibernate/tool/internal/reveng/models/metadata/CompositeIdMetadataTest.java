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
package org.hibernate.tool.internal.reveng.models.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CompositeIdMetadata}.
 *
 * @author Koen Aers
 */
public class CompositeIdMetadataTest {

	@Test
	public void testConstructorAndDefaults() {
		CompositeIdMetadata compositeId =
			new CompositeIdMetadata("id", "OrderItemId", "com.example");

		assertEquals("id", compositeId.getFieldName());
		assertEquals("OrderItemId", compositeId.getIdClassName());
		assertEquals("com.example", compositeId.getIdClassPackage());
		assertNotNull(compositeId.getAttributeOverrides());
		assertTrue(compositeId.getAttributeOverrides().isEmpty());
	}

	@Test
	public void testAddAttributeOverride() {
		CompositeIdMetadata compositeId =
			new CompositeIdMetadata("id", "OrderItemId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("productId", "PRODUCT_ID");

		List<AttributeOverrideMetadata> overrides = compositeId.getAttributeOverrides();
		assertEquals(2, overrides.size());
		assertEquals("orderId", overrides.get(0).getFieldName());
		assertEquals("ORDER_ID", overrides.get(0).getColumnName());
		assertEquals("productId", overrides.get(1).getFieldName());
		assertEquals("PRODUCT_ID", overrides.get(1).getColumnName());
	}
}
