/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.strategy;

import static org.junit.jupiter.api.Assertions.*;

import org.hibernate.tool.reveng.api.reveng.TableIdentifier;
import org.junit.jupiter.api.Test;

class TableColumnKeyTest {

	@Test
	void testEqualsSameValues() {
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		TableColumnKey a = new TableColumnKey(ti, "COL_A");
		TableColumnKey b = new TableColumnKey(ti, "COL_A");
		assertEquals(a, b);
	}

	@Test
	void testEqualsDifferentColumn() {
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		TableColumnKey a = new TableColumnKey(ti, "COL_A");
		TableColumnKey b = new TableColumnKey(ti, "COL_B");
		assertNotEquals(a, b);
	}

	@Test
	void testEqualsDifferentTable() {
		TableIdentifier ti1 = TableIdentifier.create(null, null, "TABLE_A");
		TableIdentifier ti2 = TableIdentifier.create(null, null, "TABLE_B");
		TableColumnKey a = new TableColumnKey(ti1, "COL_A");
		TableColumnKey b = new TableColumnKey(ti2, "COL_A");
		assertNotEquals(a, b);
	}

	@Test
	void testEqualsNull() {
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		TableColumnKey a = new TableColumnKey(ti, "COL_A");
		assertNotEquals(null, a);
	}

	@Test
	void testEqualsSameInstance() {
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		TableColumnKey a = new TableColumnKey(ti, "COL_A");
		assertEquals(a, a);
	}

	@Test
	void testHashCodeConsistent() {
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		TableColumnKey a = new TableColumnKey(ti, "COL_A");
		TableColumnKey b = new TableColumnKey(ti, "COL_A");
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void testEqualsNullColumnName() {
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		TableColumnKey a = new TableColumnKey(ti, null);
		TableColumnKey b = new TableColumnKey(ti, null);
		assertEquals(a, b);
	}

	@Test
	void testEqualsNullColumnVsNonNull() {
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		TableColumnKey a = new TableColumnKey(ti, null);
		TableColumnKey b = new TableColumnKey(ti, "COL_A");
		assertNotEquals(a, b);
	}

	@Test
	void testEqualsNullTable() {
		TableColumnKey a = new TableColumnKey(null, "COL_A");
		TableColumnKey b = new TableColumnKey(null, "COL_A");
		assertEquals(a, b);
	}

	@Test
	void testEqualsNullTableVsNonNull() {
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		TableColumnKey a = new TableColumnKey(null, "COL_A");
		TableColumnKey b = new TableColumnKey(ti, "COL_A");
		assertNotEquals(a, b);
	}

	@Test
	void testEqualsDifferentClass() {
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		TableColumnKey a = new TableColumnKey(ti, "COL_A");
		assertNotEquals("not a key", a);
	}

	@Test
	void testWithCatalogAndSchema() {
		TableIdentifier ti1 = TableIdentifier.create("CAT", "SCH", "MY_TABLE");
		TableIdentifier ti2 = TableIdentifier.create("CAT", "SCH", "MY_TABLE");
		TableColumnKey a = new TableColumnKey(ti1, "COL_A");
		TableColumnKey b = new TableColumnKey(ti2, "COL_A");
		assertEquals(a, b);
	}
}
