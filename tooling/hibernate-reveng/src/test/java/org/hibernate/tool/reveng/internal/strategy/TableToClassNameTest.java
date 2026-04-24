/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.strategy;

import static org.junit.jupiter.api.Assertions.*;

import org.hibernate.tool.reveng.api.reveng.TableIdentifier;
import org.junit.jupiter.api.Test;

class TableToClassNameTest {

	@Test
	void testGetReturnsNullForUnknownTable() {
		TableToClassName map = new TableToClassName();
		assertNull(map.get(TableIdentifier.create(null, null, "UNKNOWN")));
	}

	@Test
	void testPutAndGetSimpleClassName() {
		TableToClassName map = new TableToClassName();
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		map.put(ti, "MyEntity");
		assertEquals("MyEntity", map.get(ti));
	}

	@Test
	void testPutAndGetQualifiedClassName() {
		TableToClassName map = new TableToClassName();
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		map.put(ti, "com.example.MyEntity");
		assertEquals("com.example.MyEntity", map.get(ti));
	}

	@Test
	void testMatchesByTableNameOnly() {
		TableToClassName map = new TableToClassName();
		TableIdentifier stored = TableIdentifier.create(null, null, "MY_TABLE");
		map.put(stored, "MyEntity");
		TableIdentifier lookup = TableIdentifier.create(null, null, "MY_TABLE");
		assertEquals("MyEntity", map.get(lookup));
	}

	@Test
	void testMatchesByTableNameIgnoringNullCatalog() {
		TableToClassName map = new TableToClassName();
		TableIdentifier stored = TableIdentifier.create("CAT", null, "MY_TABLE");
		map.put(stored, "MyEntity");
		TableIdentifier lookup = TableIdentifier.create(null, null, "MY_TABLE");
		assertEquals("MyEntity", map.get(lookup));
	}

	@Test
	void testMatchesByTableNameIgnoringNullSchema() {
		TableToClassName map = new TableToClassName();
		TableIdentifier stored = TableIdentifier.create(null, "SCH", "MY_TABLE");
		map.put(stored, "MyEntity");
		TableIdentifier lookup = TableIdentifier.create(null, null, "MY_TABLE");
		assertEquals("MyEntity", map.get(lookup));
	}

	@Test
	void testMatchesWithCatalogAndSchema() {
		TableToClassName map = new TableToClassName();
		TableIdentifier stored = TableIdentifier.create("CAT", "SCH", "MY_TABLE");
		map.put(stored, "MyEntity");
		TableIdentifier lookup = TableIdentifier.create("CAT", "SCH", "MY_TABLE");
		assertEquals("MyEntity", map.get(lookup));
	}

	@Test
	void testNoMatchDifferentCatalog() {
		TableToClassName map = new TableToClassName();
		TableIdentifier stored = TableIdentifier.create("CAT_A", null, "MY_TABLE");
		map.put(stored, "MyEntity");
		TableIdentifier lookup = TableIdentifier.create("CAT_B", null, "MY_TABLE");
		assertNull(map.get(lookup));
	}

	@Test
	void testNoMatchDifferentSchema() {
		TableToClassName map = new TableToClassName();
		TableIdentifier stored = TableIdentifier.create(null, "SCH_A", "MY_TABLE");
		map.put(stored, "MyEntity");
		TableIdentifier lookup = TableIdentifier.create(null, "SCH_B", "MY_TABLE");
		assertNull(map.get(lookup));
	}

	@Test
	void testNoMatchDifferentTableName() {
		TableToClassName map = new TableToClassName();
		TableIdentifier stored = TableIdentifier.create(null, null, "TABLE_A");
		map.put(stored, "EntityA");
		TableIdentifier lookup = TableIdentifier.create(null, null, "TABLE_B");
		assertNull(map.get(lookup));
	}

	@Test
	void testPackageExtractedFromQualifiedName() {
		TableToClassName map = new TableToClassName();
		TableIdentifier ti = TableIdentifier.create(null, null, "MY_TABLE");
		map.put(ti, "com.example.model.MyEntity");
		assertEquals("com.example.model.MyEntity", map.get(ti));
	}
}
