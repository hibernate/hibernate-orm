/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.dialect;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for pure logic methods in AbstractMetaDataDialect.
 * Uses a minimal concrete subclass to avoid needing a database connection.
 */
public class AbstractMetaDataDialectTest {

	/**
	 * Minimal concrete implementation that stubs out methods requiring a database.
	 */
	private static class TestableMetaDataDialect extends AbstractMetaDataDialect {
		@Override
		public Iterator<Map<String, Object>> getTables(String catalog, String schema, String table) {
			return Collections.emptyIterator();
		}

		@Override
		public Iterator<Map<String, Object>> getColumns(String catalog, String schema, String table, String column) {
			return Collections.emptyIterator();
		}

		@Override
		public Iterator<Map<String, Object>> getIndexInfo(String catalog, String schema, String table) {
			return Collections.emptyIterator();
		}

		@Override
		public Iterator<Map<String, Object>> getPrimaryKeys(String catalog, String schema, String table) {
			return Collections.emptyIterator();
		}

		@Override
		public Iterator<Map<String, Object>> getExportedKeys(String catalog, String schema, String table) {
			return Collections.emptyIterator();
		}
	}

	@Test
	public void testNeedQuoteNull() {
		TestableMetaDataDialect dialect = new TestableMetaDataDialect();
		assertFalse(dialect.needQuote(null));
	}

	@Test
	public void testNeedQuoteSimpleName() {
		TestableMetaDataDialect dialect = new TestableMetaDataDialect();
		assertFalse(dialect.needQuote("PERSON"));
	}

	@Test
	public void testNeedQuoteWithHyphen() {
		TestableMetaDataDialect dialect = new TestableMetaDataDialect();
		assertTrue(dialect.needQuote("my-table"));
	}

	@Test
	public void testNeedQuoteWithSpace() {
		TestableMetaDataDialect dialect = new TestableMetaDataDialect();
		assertTrue(dialect.needQuote("my table"));
	}

	@Test
	public void testNeedQuoteWithDot() {
		TestableMetaDataDialect dialect = new TestableMetaDataDialect();
		assertTrue(dialect.needQuote("schema.table"));
	}

	@Test
	public void testNeedQuoteNoSpecialChars() {
		TestableMetaDataDialect dialect = new TestableMetaDataDialect();
		assertFalse(dialect.needQuote("SIMPLE_NAME_123"));
	}

	@Test
	public void testGetSuggestedPrimaryKeyStrategyName() {
		TestableMetaDataDialect dialect = new TestableMetaDataDialect();
		Iterator<Map<String, Object>> iter = dialect.getSuggestedPrimaryKeyStrategyName("cat", "schema", "table");
		assertTrue(iter.hasNext());
		Map<String, Object> result = iter.next();
		assertEquals("cat", result.get("TABLE_CAT"));
		assertEquals("schema", result.get("TABLE_SCHEMA"));
		assertEquals("table", result.get("TABLE_NAME"));
		assertNull(result.get("HIBERNATE_STRATEGY"));
		assertFalse(iter.hasNext());
	}

	@Test
	public void testGetSuggestedPrimaryKeyStrategyNameNulls() {
		TestableMetaDataDialect dialect = new TestableMetaDataDialect();
		Iterator<Map<String, Object>> iter = dialect.getSuggestedPrimaryKeyStrategyName(null, null, null);
		assertTrue(iter.hasNext());
		Map<String, Object> result = iter.next();
		assertNull(result.get("TABLE_CAT"));
		assertNull(result.get("TABLE_SCHEMA"));
		assertNull(result.get("TABLE_NAME"));
	}

	@Test
	public void testCloseIteratorNonResultSetIterator() {
		TestableMetaDataDialect dialect = new TestableMetaDataDialect();
		Iterator<String> plainIterator = Collections.<String>emptyList().iterator();
		dialect.close(plainIterator);
	}

	@Test
	public void testCloseNullConnectionProvider() {
		TestableMetaDataDialect dialect = new TestableMetaDataDialect();
		dialect.close();
	}
}
