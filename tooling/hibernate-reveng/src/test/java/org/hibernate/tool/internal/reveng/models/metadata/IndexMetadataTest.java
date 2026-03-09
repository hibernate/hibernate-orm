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

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IndexMetadata}.
 *
 * @author Koen Aers
 */
public class IndexMetadataTest {

	@Test
	public void testConstructorAndDefaults() {
		IndexMetadata index = new IndexMetadata("IDX_NAME", true);

		assertEquals("IDX_NAME", index.getIndexName());
		assertTrue(index.isUnique());
		assertNotNull(index.getColumnNames());
		assertTrue(index.getColumnNames().isEmpty());
	}

	@Test
	public void testNonUniqueIndex() {
		IndexMetadata index = new IndexMetadata("IDX_SEARCH", false);

		assertFalse(index.isUnique());
	}

	@Test
	public void testAddColumns() {
		IndexMetadata index = new IndexMetadata("IDX_COMPOSITE", true)
			.addColumn("COL_A")
			.addColumn("COL_B");

		assertEquals(2, index.getColumnNames().size());
		assertEquals("COL_A", index.getColumnNames().get(0));
		assertEquals("COL_B", index.getColumnNames().get(1));
	}

	@Test
	public void testSingleColumnIndex() {
		IndexMetadata index = new IndexMetadata("IDX_EMAIL", true)
			.addColumn("EMAIL");

		assertEquals(1, index.getColumnNames().size());
		assertEquals("EMAIL", index.getColumnNames().get(0));
	}
}
