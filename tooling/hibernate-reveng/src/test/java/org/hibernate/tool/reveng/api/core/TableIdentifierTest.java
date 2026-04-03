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

public class TableIdentifierTest {

	@Test
	public void testCreateWithAllParts() {
		TableIdentifier id = TableIdentifier.create("MY_CAT", "MY_SCH", "MY_TABLE");
		assertEquals("MY_CAT", id.getCatalog());
		assertEquals("MY_SCH", id.getSchema());
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
		TableIdentifier id1 = TableIdentifier.create("CAT", "SCH", "TAB");
		TableIdentifier id2 = TableIdentifier.create("CAT", "SCH", "TAB");
		assertEquals(id1, id2);
		assertEquals(id1.hashCode(), id2.hashCode());
	}

	@Test
	public void testEqualsDifferentCatalog() {
		TableIdentifier id1 = TableIdentifier.create("CAT1", "SCH", "TAB");
		TableIdentifier id2 = TableIdentifier.create("CAT2", "SCH", "TAB");
		assertNotEquals(id1, id2);
	}

	@Test
	public void testEqualsDifferentSchema() {
		TableIdentifier id1 = TableIdentifier.create("CAT", "SCH1", "TAB");
		TableIdentifier id2 = TableIdentifier.create("CAT", "SCH2", "TAB");
		assertNotEquals(id1, id2);
	}

	@Test
	public void testEqualsDifferentName() {
		TableIdentifier id1 = TableIdentifier.create("CAT", "SCH", "TAB1");
		TableIdentifier id2 = TableIdentifier.create("CAT", "SCH", "TAB2");
		assertNotEquals(id1, id2);
	}

	@Test
	public void testEqualsWithNulls() {
		TableIdentifier id1 = TableIdentifier.create(null, null, "TAB");
		TableIdentifier id2 = TableIdentifier.create(null, null, "TAB");
		assertEquals(id1, id2);
	}

	@Test
	public void testEqualsNullVsNonNull() {
		TableIdentifier id1 = TableIdentifier.create(null, null, "TAB");
		TableIdentifier id2 = TableIdentifier.create("CAT", null, "TAB");
		assertNotEquals(id1, id2);
	}

	@Test
	public void testEqualsNotTableIdentifier() {
		TableIdentifier id = TableIdentifier.create("CAT", "SCH", "TAB");
		assertFalse(id.equals("not a TableIdentifier"));
	}

	@Test
	public void testToStringAllParts() {
		TableIdentifier id = TableIdentifier.create("MY_CAT", "MY_SCH", "MY_TABLE");
		assertEquals("TableIdentifier(MY_CAT.MY_SCH.MY_TABLE)", id.toString());
	}

	@Test
	public void testToStringNoCatalog() {
		TableIdentifier id = TableIdentifier.create(null, "MY_SCH", "MY_TABLE");
		assertEquals("TableIdentifier(MY_SCH.MY_TABLE)", id.toString());
	}

	@Test
	public void testToStringNoSchema() {
		TableIdentifier id = TableIdentifier.create("MY_CAT", null, "MY_TABLE");
		assertEquals("TableIdentifier(MY_CAT.MY_TABLE)", id.toString());
	}

	@Test
	public void testToStringTableOnly() {
		TableIdentifier id = TableIdentifier.create(null, null, "MY_TABLE");
		assertEquals("TableIdentifier(MY_TABLE)", id.toString());
	}

	@Test
	public void testHashCodeConsistency() {
		TableIdentifier id = TableIdentifier.create("CAT", "SCH", "TAB");
		int hash1 = id.hashCode();
		int hash2 = id.hashCode();
		assertEquals(hash1, hash2);
	}

	@Test
	public void testHashCodeWithNulls() {
		TableIdentifier id = TableIdentifier.create(null, null, null);
		id.hashCode();
	}
}
