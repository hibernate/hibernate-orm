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
 * Tests for {@link EmbeddedFieldMetadata}.
 *
 * @author Koen Aers
 */
public class EmbeddedFieldMetadataTest {

	@Test
	public void testConstructorAndDefaults() {
		EmbeddedFieldMetadata embedded =
			new EmbeddedFieldMetadata("address", "Address", "com.example");

		assertEquals("address", embedded.getFieldName());
		assertEquals("Address", embedded.getEmbeddableClassName());
		assertEquals("com.example", embedded.getEmbeddablePackage());
		assertNotNull(embedded.getAttributeOverrides());
		assertTrue(embedded.getAttributeOverrides().isEmpty());
	}

	@Test
	public void testAddAttributeOverride() {
		EmbeddedFieldMetadata embedded =
			new EmbeddedFieldMetadata("address", "Address", "com.example")
				.addAttributeOverride("street", "HOME_STREET")
				.addAttributeOverride("city", "HOME_CITY");

		List<AttributeOverrideMetadata> overrides = embedded.getAttributeOverrides();
		assertEquals(2, overrides.size());
		assertEquals("street", overrides.get(0).getFieldName());
		assertEquals("HOME_STREET", overrides.get(0).getColumnName());
		assertEquals("city", overrides.get(1).getFieldName());
		assertEquals("HOME_CITY", overrides.get(1).getColumnName());
	}
}
