/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IndexDescriptor}.
 *
 * @author Koen Aers
 */
public class IndexDescriptorTest {

	@Test
	public void testConstructorAndDefaults() {
		IndexDescriptor index = new IndexDescriptor("IDX_NAME", true);

		assertEquals("IDX_NAME", index.getIndexName());
		assertTrue(index.isUnique());
		assertNotNull(index.getColumnNames());
		assertTrue(index.getColumnNames().isEmpty());
	}

	@Test
	public void testNonUniqueIndex() {
		IndexDescriptor index = new IndexDescriptor("IDX_SEARCH", false);

		assertFalse(index.isUnique());
	}

	@Test
	public void testAddColumns() {
		IndexDescriptor index = new IndexDescriptor("IDX_COMPOSITE", true)
			.addColumn("COL_A")
			.addColumn("COL_B");

		assertEquals(2, index.getColumnNames().size());
		assertEquals("COL_A", index.getColumnNames().get(0));
		assertEquals("COL_B", index.getColumnNames().get(1));
	}

	@Test
	public void testSingleColumnIndex() {
		IndexDescriptor index = new IndexDescriptor("IDX_EMAIL", true)
			.addColumn("EMAIL");

		assertEquals(1, index.getColumnNames().size());
		assertEquals("EMAIL", index.getColumnNames().get(0));
	}
}
