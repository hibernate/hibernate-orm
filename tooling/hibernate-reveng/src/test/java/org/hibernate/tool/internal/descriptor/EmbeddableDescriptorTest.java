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
package org.hibernate.tool.internal.descriptor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EmbeddableDescriptor}.
 *
 * @author Koen Aers
 */
public class EmbeddableDescriptorTest {

	@Test
	public void testConstructorAndDefaults() {
		EmbeddableDescriptor embeddable = new EmbeddableDescriptor("Address", "com.example");

		assertEquals("Address", embeddable.getClassName());
		assertEquals("com.example", embeddable.getPackageName());
		assertNotNull(embeddable.getColumns());
		assertTrue(embeddable.getColumns().isEmpty());
	}

	@Test
	public void testAddColumn() {
		EmbeddableDescriptor embeddable = new EmbeddableDescriptor("Address", "com.example")
			.addColumn(new ColumnDescriptor("STREET", "street", String.class))
			.addColumn(new ColumnDescriptor("CITY", "city", String.class));

		assertEquals(2, embeddable.getColumns().size());
		assertEquals("STREET", embeddable.getColumns().get(0).getColumnName());
		assertEquals("CITY", embeddable.getColumns().get(1).getColumnName());
	}
}
