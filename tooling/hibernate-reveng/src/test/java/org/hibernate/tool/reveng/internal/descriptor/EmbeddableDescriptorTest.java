/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

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
