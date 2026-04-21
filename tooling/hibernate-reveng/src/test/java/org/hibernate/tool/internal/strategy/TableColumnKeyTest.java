/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.strategy;

import static org.junit.jupiter.api.Assertions.*;

import org.hibernate.tool.api.reveng.TableIdentifier;
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
