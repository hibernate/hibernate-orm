/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EmbeddedFieldDescriptor}.
 *
 * @author Koen Aers
 */
public class EmbeddedFieldDescriptorTest {

	@Test
	public void testConstructorAndDefaults() {
		EmbeddedFieldDescriptor embedded =
			new EmbeddedFieldDescriptor("address", "Address", "com.example");

		assertEquals("address", embedded.getFieldName());
		assertEquals("Address", embedded.getEmbeddableClassName());
		assertEquals("com.example", embedded.getEmbeddablePackage());
		assertNotNull(embedded.getAttributeOverrides());
		assertTrue(embedded.getAttributeOverrides().isEmpty());
	}

	@Test
	public void testAddAttributeOverride() {
		EmbeddedFieldDescriptor embedded =
			new EmbeddedFieldDescriptor("address", "Address", "com.example")
				.addAttributeOverride("street", "HOME_STREET")
				.addAttributeOverride("city", "HOME_CITY");

		List<AttributeOverrideDescriptor> overrides = embedded.getAttributeOverrides();
		assertEquals(2, overrides.size());
		assertEquals("street", overrides.get(0).getFieldName());
		assertEquals("HOME_STREET", overrides.get(0).getColumnName());
		assertEquals("city", overrides.get(1).getFieldName());
		assertEquals("HOME_CITY", overrides.get(1).getColumnName());
	}
}
