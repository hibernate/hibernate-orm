/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableFilterTest {

	@Test
	public void testDefaultFilterMatchesAll() {
		TableFilter filter = new TableFilter();
		assertEquals(".*", filter.getMatchCatalog());
		assertEquals(".*", filter.getMatchSchema());
		assertEquals(".*", filter.getMatchName());
		assertNull(filter.getExclude());
	}

	@Test
	public void testExcludeReturnsNullWhenNotRelevant() {
		TableFilter filter = new TableFilter();
		filter.setMatchName("PERSON");
		filter.setExclude(Boolean.TRUE);
		TableIdentifier ti = TableIdentifier.create("catalog", "schema", "OTHER");
		assertNull(filter.exclude(ti));
	}

	@Test
	public void testExcludeReturnsTrueWhenRelevant() {
		TableFilter filter = new TableFilter();
		filter.setMatchName("PERSON");
		filter.setExclude(Boolean.TRUE);
		TableIdentifier ti = TableIdentifier.create("catalog", "schema", "PERSON");
		assertEquals(Boolean.TRUE, filter.exclude(ti));
	}

	@Test
	public void testExcludeReturnsFalseWhenExplicitlyIncluded() {
		TableFilter filter = new TableFilter();
		filter.setMatchName("PERSON");
		filter.setExclude(Boolean.FALSE);
		TableIdentifier ti = TableIdentifier.create("catalog", "schema", "PERSON");
		assertEquals(Boolean.FALSE, filter.exclude(ti));
	}

	@Test
	public void testMatcherEquals() {
		TableFilter filter = new TableFilter();
		filter.setMatchName("PERSON");
		TableIdentifier match = TableIdentifier.create("cat", "sch", "PERSON");
		TableIdentifier noMatch = TableIdentifier.create("cat", "sch", "ADDRESS");
		filter.setExclude(Boolean.TRUE);
		assertNotNull(filter.exclude(match));
		assertNull(filter.exclude(noMatch));
	}

	@Test
	public void testMatcherStartsWith() {
		TableFilter filter = new TableFilter();
		filter.setMatchName("PERSON.*");
		filter.setExclude(Boolean.TRUE);
		TableIdentifier match = TableIdentifier.create("cat", "sch", "PERSON_ADDRESS");
		TableIdentifier noMatch = TableIdentifier.create("cat", "sch", "ADDRESS");
		assertNotNull(filter.exclude(match));
		assertNull(filter.exclude(noMatch));
	}

	@Test
	public void testMatcherEndsWith() {
		TableFilter filter = new TableFilter();
		filter.setMatchName(".*_LOG");
		filter.setExclude(Boolean.TRUE);
		TableIdentifier match = TableIdentifier.create("cat", "sch", "AUDIT_LOG");
		TableIdentifier noMatch = TableIdentifier.create("cat", "sch", "PERSON");
		assertNotNull(filter.exclude(match));
		assertNull(filter.exclude(noMatch));
	}

	@Test
	public void testMatcherSubstring() {
		TableFilter filter = new TableFilter();
		filter.setMatchName(".*AUDIT.*");
		filter.setExclude(Boolean.TRUE);
		TableIdentifier match = TableIdentifier.create("cat", "sch", "MY_AUDIT_LOG");
		TableIdentifier noMatch = TableIdentifier.create("cat", "sch", "PERSON");
		assertNotNull(filter.exclude(match));
		assertNull(filter.exclude(noMatch));
	}

	@Test
	public void testMatcherAny() {
		TableFilter filter = new TableFilter();
		// default is ".*" which matches anything
		filter.setExclude(Boolean.TRUE);
		TableIdentifier ti = TableIdentifier.create("cat", "sch", "ANYTHING");
		assertEquals(Boolean.TRUE, filter.exclude(ti));
	}

	@Test
	public void testCatalogAndSchemaMatching() {
		TableFilter filter = new TableFilter();
		filter.setMatchCatalog("MY_CAT");
		filter.setMatchSchema("MY_SCH");
		filter.setMatchName(".*");
		filter.setExclude(Boolean.TRUE);

		TableIdentifier match = TableIdentifier.create("MY_CAT", "MY_SCH", "TABLE");
		TableIdentifier wrongCat = TableIdentifier.create("OTHER", "MY_SCH", "TABLE");
		TableIdentifier wrongSch = TableIdentifier.create("MY_CAT", "OTHER", "TABLE");

		assertNotNull(filter.exclude(match));
		assertNull(filter.exclude(wrongCat));
		assertNull(filter.exclude(wrongSch));
	}

	@Test
	public void testPackageReturnsNullWhenNotRelevant() {
		TableFilter filter = new TableFilter();
		filter.setMatchName("PERSON");
		filter.setPackage("com.example");
		TableIdentifier other = TableIdentifier.create("cat", "sch", "OTHER");
		assertNull(filter.getPackage(other));
	}

	@Test
	public void testPackageReturnsValueWhenRelevant() {
		TableFilter filter = new TableFilter();
		filter.setMatchName("PERSON");
		filter.setPackage("com.example");
		TableIdentifier match = TableIdentifier.create("cat", "sch", "PERSON");
		assertEquals("com.example", filter.getPackage(match));
	}

	@Test
	public void testMetaAttributesReturnsNullWhenNotRelevant() {
		TableFilter filter = new TableFilter();
		filter.setMatchName("PERSON");
		TableIdentifier other = TableIdentifier.create("cat", "sch", "OTHER");
		assertNull(filter.getMetaAttributes(other));
	}

	@Test
	public void testToString() {
		TableFilter filter = new TableFilter();
		filter.setMatchCatalog("CAT");
		filter.setMatchSchema("SCH");
		filter.setMatchName("TBL");
		filter.setExclude(Boolean.TRUE);
		String str = filter.toString();
		assertTrue(str.contains("CAT"));
		assertTrue(str.contains("SCH"));
		assertTrue(str.contains("TBL"));
	}

	@Test
	public void testMatcherToString() {
		TableFilter.Matcher matcher = new TableFilter.Matcher("PERSON.*");
		assertEquals("PERSON.*", matcher.toString());
	}
}
