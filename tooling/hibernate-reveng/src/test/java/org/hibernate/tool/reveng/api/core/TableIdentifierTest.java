/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableIdentifierTest {

	@Test
	public void testCreateWithAllParts() {
		TableIdentifier id = TableIdentifier.create("MY_CATALOG", "MY_SCHEMA", "MY_TABLE");
		assertEquals("MY_CATALOG", id.getCatalog());
		assertEquals("MY_SCHEMA", id.getSchema());
		assertEquals("MY_TABLE", id.getName());
	}

	@Test
	public void testCreateWithNulls() {
		TableIdentifier id = TableIdentifier.create(null, null, "MY_TABLE");
		assertNull(id.getCatalog());
		assertNull(id.getSchema());
		assertEquals("MY_TABLE", id.getName());
	}

	@Test
	public void testEqualsSameValues() {
		TableIdentifier id1 = TableIdentifier.create("cat", "sch", "tbl");
		TableIdentifier id2 = TableIdentifier.create("cat", "sch", "tbl");
		assertEquals(id1, id2);
		assertEquals(id1.hashCode(), id2.hashCode());
	}

	@Test
	public void testEqualsDifferentName() {
		TableIdentifier id1 = TableIdentifier.create("cat", "sch", "tbl1");
		TableIdentifier id2 = TableIdentifier.create("cat", "sch", "tbl2");
		assertNotEquals(id1, id2);
	}

	@Test
	public void testEqualsDifferentSchema() {
		TableIdentifier id1 = TableIdentifier.create("cat", "sch1", "tbl");
		TableIdentifier id2 = TableIdentifier.create("cat", "sch2", "tbl");
		assertNotEquals(id1, id2);
	}

	@Test
	public void testEqualsDifferentCatalog() {
		TableIdentifier id1 = TableIdentifier.create("cat1", "sch", "tbl");
		TableIdentifier id2 = TableIdentifier.create("cat2", "sch", "tbl");
		assertNotEquals(id1, id2);
	}

	@Test
	public void testEqualsWithNulls() {
		TableIdentifier id1 = TableIdentifier.create(null, null, "tbl");
		TableIdentifier id2 = TableIdentifier.create(null, null, "tbl");
		assertEquals(id1, id2);
		assertEquals(id1.hashCode(), id2.hashCode());
	}

	@Test
	public void testEqualsNullVsNonNull() {
		TableIdentifier id1 = TableIdentifier.create(null, "sch", "tbl");
		TableIdentifier id2 = TableIdentifier.create("cat", "sch", "tbl");
		assertNotEquals(id1, id2);
	}

	@Test
	public void testNotEqualsNull() {
		TableIdentifier id = TableIdentifier.create("cat", "sch", "tbl");
		assertFalse(id.equals(null));
	}

	@Test
	public void testNotEqualsOtherType() {
		TableIdentifier id = TableIdentifier.create("cat", "sch", "tbl");
		assertFalse(id.equals("not a TableIdentifier"));
	}

	@Test
	public void testEqualsSelf() {
		TableIdentifier id = TableIdentifier.create("cat", "sch", "tbl");
		assertEquals(id, id);
	}

	@Test
	public void testToStringFull() {
		TableIdentifier id = TableIdentifier.create("MY_CATALOG", "MY_SCHEMA", "MY_TABLE");
		String str = id.toString();
		assertTrue(str.contains("MY_CATALOG"));
		assertTrue(str.contains("MY_SCHEMA"));
		assertTrue(str.contains("MY_TABLE"));
		assertTrue(str.startsWith("TableIdentifier("));
	}

	@Test
	public void testToStringNameOnly() {
		TableIdentifier id = TableIdentifier.create(null, null, "MY_TABLE");
		String str = id.toString();
		assertTrue(str.contains("MY_TABLE"));
		assertFalse(str.contains("null"));
	}

	@Test
	public void testToStringSchemaAndName() {
		TableIdentifier id = TableIdentifier.create(null, "MY_SCHEMA", "MY_TABLE");
		String str = id.toString();
		assertTrue(str.contains("MY_SCHEMA"));
		assertTrue(str.contains("MY_TABLE"));
	}

	@Test
	public void testHashCodeConsistency() {
		TableIdentifier id = TableIdentifier.create("cat", "sch", "tbl");
		int hash1 = id.hashCode();
		int hash2 = id.hashCode();
		assertEquals(hash1, hash2);
	}

	@Test
	public void testHashCodeWithAllNulls() {
		TableIdentifier id = TableIdentifier.create(null, null, null);
		// Should not throw
		id.hashCode();
	}
}
