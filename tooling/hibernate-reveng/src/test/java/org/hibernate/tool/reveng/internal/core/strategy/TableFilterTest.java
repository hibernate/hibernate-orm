/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableFilterTest {

	// --- Matcher tests ---

	@Test
	public void testMatcherEquals() {
		TableFilter.Matcher m = new TableFilter.Matcher("MY_TABLE");
		assertTrue(m.match("MY_TABLE"));
		assertFalse(m.match("OTHER"));
		assertFalse(m.match("MY_TABLE_EXT"));
	}

	@Test
	public void testMatcherAny() {
		TableFilter.Matcher m = new TableFilter.Matcher(".*");
		assertTrue(m.match("anything"));
		assertTrue(m.match(""));
	}

	@Test
	public void testMatcherStartsWith() {
		TableFilter.Matcher m = new TableFilter.Matcher("MY_.*");
		assertTrue(m.match("MY_TABLE"));
		assertTrue(m.match("MY_OTHER"));
		assertFalse(m.match("YOUR_TABLE"));
	}

	@Test
	public void testMatcherEndsWith() {
		TableFilter.Matcher m = new TableFilter.Matcher(".*_TABLE");
		assertTrue(m.match("MY_TABLE"));
		assertTrue(m.match("YOUR_TABLE"));
		assertFalse(m.match("MY_VIEW"));
	}

	@Test
	public void testMatcherSubstring() {
		TableFilter.Matcher m = new TableFilter.Matcher(".*EMPL.*");
		assertTrue(m.match("EMPLOYEE"));
		assertTrue(m.match("MY_EMPLOYEE_TABLE"));
		assertFalse(m.match("DEPARTMENT"));
	}

	@Test
	public void testMatcherToString() {
		TableFilter.Matcher m = new TableFilter.Matcher("MY_.*");
		assertEquals("MY_.*", m.toString());
	}

	// --- TableFilter tests ---

	@Test
	public void testDefaultFilterMatchesAll() {
		TableFilter filter = new TableFilter();
		TableIdentifier id = TableIdentifier.create("CAT", "SCH", "TAB");
		assertNull(filter.exclude(id));
		assertEquals(".*", filter.getMatchCatalog());
		assertEquals(".*", filter.getMatchSchema());
		assertEquals(".*", filter.getMatchName());
		assertNull(filter.getExclude());
	}

	@Test
	public void testFilterExcludeTrue() {
		TableFilter filter = new TableFilter();
		filter.setExclude(true);
		TableIdentifier id = TableIdentifier.create("CAT", "SCH", "TAB");
		assertTrue(filter.exclude(id));
	}

	@Test
	public void testFilterExcludeFalse() {
		TableFilter filter = new TableFilter();
		filter.setExclude(false);
		TableIdentifier id = TableIdentifier.create("CAT", "SCH", "TAB");
		assertFalse(filter.exclude(id));
	}

	@Test
	public void testFilterNotRelevant() {
		TableFilter filter = new TableFilter();
		filter.setMatchName("EMPLOYEE");
		filter.setExclude(true);
		TableIdentifier id = TableIdentifier.create("CAT", "SCH", "DEPARTMENT");
		assertNull(filter.exclude(id));
	}

	@Test
	public void testFilterByCatalog() {
		TableFilter filter = new TableFilter();
		filter.setMatchCatalog("MY_CAT");
		filter.setExclude(true);
		assertTrue(filter.exclude(TableIdentifier.create("MY_CAT", "SCH", "TAB")));
		assertNull(filter.exclude(TableIdentifier.create("OTHER", "SCH", "TAB")));
	}

	@Test
	public void testFilterBySchema() {
		TableFilter filter = new TableFilter();
		filter.setMatchSchema("MY_SCH");
		filter.setExclude(true);
		assertTrue(filter.exclude(TableIdentifier.create("CAT", "MY_SCH", "TAB")));
		assertNull(filter.exclude(TableIdentifier.create("CAT", "OTHER", "TAB")));
	}

	@Test
	public void testFilterPackage() {
		TableFilter filter = new TableFilter();
		filter.setPackage("com.example");
		TableIdentifier id = TableIdentifier.create("CAT", "SCH", "TAB");
		assertEquals("com.example", filter.getPackage(id));
	}

	@Test
	public void testFilterPackageNotRelevant() {
		TableFilter filter = new TableFilter();
		filter.setMatchName("EMPLOYEE");
		filter.setPackage("com.example");
		assertNull(filter.getPackage(TableIdentifier.create("CAT", "SCH", "DEPARTMENT")));
	}

	@Test
	public void testFilterToString() {
		TableFilter filter = new TableFilter();
		String s = filter.toString();
		assertTrue(s.contains(".*"));
	}

	@Test
	public void testFilterWithStartsWithPattern() {
		TableFilter filter = new TableFilter();
		filter.setMatchName("EMP_.*");
		filter.setExclude(true);
		assertTrue(filter.exclude(TableIdentifier.create("CAT", "SCH", "EMP_TABLE")));
		assertNull(filter.exclude(TableIdentifier.create("CAT", "SCH", "DEP_TABLE")));
	}

	@Test
	public void testFilterMetaAttributes() {
		TableFilter filter = new TableFilter();
		assertNull(filter.getMetaAttributes(TableIdentifier.create("C", "S", "T")));
	}
}
