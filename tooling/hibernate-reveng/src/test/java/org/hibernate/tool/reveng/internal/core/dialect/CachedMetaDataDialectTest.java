/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.dialect;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.reveng.api.core.RevengDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CachedMetaDataDialectTest {

	private CachedMetaDataDialect cached;
	private StubDialect stub;

	@BeforeEach
	public void setUp() {
		stub = new StubDialect();
		cached = new CachedMetaDataDialect(stub);
	}

	@Test
	public void testGetTablesReturnsDelegateResultFirstTime() {
		stub.addTable(Map.of("TABLE_NAME", "FOO"));
		Iterator<Map<String, Object>> iter = cached.getTables(null, "PUBLIC", "FOO");
		assertTrue(iter.hasNext());
		assertEquals("FOO", iter.next().get("TABLE_NAME"));
		assertFalse(iter.hasNext());
		cached.close(iter);
	}

	@Test
	public void testGetTablesCachesOnSecondCall() {
		stub.addTable(Map.of("TABLE_NAME", "BAR"));
		// First call: hits delegate
		Iterator<Map<String, Object>> iter1 = cached.getTables(null, "PUBLIC", "BAR");
		iter1.next();
		cached.close(iter1);
		assertEquals(1, stub.getTablesCallCount);

		// Second call: should return from cache, not delegate
		Iterator<Map<String, Object>> iter2 = cached.getTables(null, "PUBLIC", "BAR");
		assertEquals(1, stub.getTablesCallCount); // delegate NOT called again
		assertTrue(iter2.hasNext());
		assertEquals("BAR", iter2.next().get("TABLE_NAME"));
	}

	@Test
	public void testGetColumnsAndCaching() {
		stub.addColumn(Map.of("COLUMN_NAME", "ID", "DATA_TYPE", "INT"));
		Iterator<Map<String, Object>> iter = cached.getColumns(null, "PUBLIC", "T", "%");
		assertTrue(iter.hasNext());
		assertEquals("ID", iter.next().get("COLUMN_NAME"));
		cached.close(iter);

		// Second call uses cache
		Iterator<Map<String, Object>> iter2 = cached.getColumns(null, "PUBLIC", "T", "%");
		assertEquals(1, stub.getColumnsCallCount);
		assertTrue(iter2.hasNext());
	}

	@Test
	public void testGetPrimaryKeysAndCaching() {
		stub.addPrimaryKey(Map.of("COLUMN_NAME", "ID"));
		Iterator<Map<String, Object>> iter = cached.getPrimaryKeys(null, "PUBLIC", "T");
		assertTrue(iter.hasNext());
		assertEquals("ID", iter.next().get("COLUMN_NAME"));
		cached.close(iter);

		Iterator<Map<String, Object>> iter2 = cached.getPrimaryKeys(null, "PUBLIC", "T");
		assertEquals(1, stub.getPrimaryKeysCallCount);
		assertTrue(iter2.hasNext());
	}

	@Test
	public void testGetExportedKeysAndCaching() {
		stub.addExportedKey(Map.of("FK_NAME", "fk_1"));
		Iterator<Map<String, Object>> iter = cached.getExportedKeys(null, "PUBLIC", "T");
		assertTrue(iter.hasNext());
		iter.next(); // fully consume
		cached.close(iter);

		cached.getExportedKeys(null, "PUBLIC", "T");
		assertEquals(1, stub.getExportedKeysCallCount);
	}

	@Test
	public void testGetIndexInfoAndCaching() {
		stub.addIndexInfo(Map.of("INDEX_NAME", "idx_1"));
		Iterator<Map<String, Object>> iter = cached.getIndexInfo(null, "PUBLIC", "T");
		assertTrue(iter.hasNext());
		iter.next(); // fully consume
		cached.close(iter);

		cached.getIndexInfo(null, "PUBLIC", "T");
		assertEquals(1, stub.getIndexInfoCallCount);
	}

	@Test
	public void testGetSuggestedPrimaryKeyStrategyNameAndCaching() {
		stub.addPkStrategy(Map.of("HIBERNATE_STRATEGY", "identity"));
		Iterator<Map<String, Object>> iter = cached.getSuggestedPrimaryKeyStrategyName(null, "PUBLIC", "T");
		assertTrue(iter.hasNext());
		assertEquals("identity", iter.next().get("HIBERNATE_STRATEGY"));
		cached.close(iter);

		cached.getSuggestedPrimaryKeyStrategyName(null, "PUBLIC", "T");
		assertEquals(1, stub.getPkStrategyCallCount);
	}

	@Test
	public void testNeedQuoteDelegatesToDelegate() {
		assertTrue(cached.needQuote("order"));
		assertFalse(cached.needQuote("NORMAL"));
	}

	@Test
	public void testCloseWithNonCachedIterator() {
		// Closing a plain iterator delegates to the stub
		Iterator<Map<String, Object>> plain = List.<Map<String, Object>>of().iterator();
		cached.close(plain);
		assertTrue(stub.closeCalled);
	}

	@Test
	public void testDifferentKeysCacheSeparately() {
		stub.addTable(Map.of("TABLE_NAME", "A"));
		Iterator<Map<String, Object>> iter1 = cached.getTables(null, "PUBLIC", "A");
		iter1.next();
		cached.close(iter1);

		// Different key - should call delegate again
		stub.tables.clear();
		stub.addTable(Map.of("TABLE_NAME", "B"));
		Iterator<Map<String, Object>> iter2 = cached.getTables(null, "PUBLIC", "B");
		assertEquals(2, stub.getTablesCallCount);
		assertTrue(iter2.hasNext());
		assertEquals("B", iter2.next().get("TABLE_NAME"));
		cached.close(iter2);
	}

	@Test
	public void testStoreThrowsIfNotFullyConsumed() {
		stub.addTable(Map.of("TABLE_NAME", "X"));
		stub.addTable(Map.of("TABLE_NAME", "Y"));
		Iterator<Map<String, Object>> iter = cached.getTables(null, "PUBLIC", "%");
		iter.next(); // consume only one of two
		assertThrows(IllegalStateException.class, () -> cached.close(iter));
	}

	@Test
	public void testCloseDelegatesToDelegate() {
		cached.close();
		assertTrue(stub.closeDialectCalled);
	}

	@Test
	public void testConfigureDelegatesToDelegate() {
		cached.configure(null);
		assertTrue(stub.configureCalled);
	}

	/**
	 * A minimal RevengDialect stub that returns pre-configured data
	 * from in-memory lists. No database connection needed.
	 */
	private static class StubDialect implements RevengDialect {
		final List<Map<String, Object>> tables = new ArrayList<>();
		final List<Map<String, Object>> columns = new ArrayList<>();
		final List<Map<String, Object>> primaryKeys = new ArrayList<>();
		final List<Map<String, Object>> exportedKeys = new ArrayList<>();
		final List<Map<String, Object>> indexInfo = new ArrayList<>();
		final List<Map<String, Object>> pkStrategies = new ArrayList<>();

		int getTablesCallCount;
		int getColumnsCallCount;
		int getPrimaryKeysCallCount;
		int getExportedKeysCallCount;
		int getIndexInfoCallCount;
		int getPkStrategyCallCount;
		boolean closeCalled;
		boolean closeDialectCalled;
		boolean configureCalled;

		void addTable(Map<String, Object> row) { tables.add(new HashMap<>(row)); }
		void addColumn(Map<String, Object> row) { columns.add(new HashMap<>(row)); }
		void addPrimaryKey(Map<String, Object> row) { primaryKeys.add(new HashMap<>(row)); }
		void addExportedKey(Map<String, Object> row) { exportedKeys.add(new HashMap<>(row)); }
		void addIndexInfo(Map<String, Object> row) { indexInfo.add(new HashMap<>(row)); }
		void addPkStrategy(Map<String, Object> row) { pkStrategies.add(new HashMap<>(row)); }

		@Override
		public void configure(ConnectionProvider connectionProvider) {
			configureCalled = true;
		}

		@Override
		public void close() {
			closeDialectCalled = true;
		}

		@Override
		public void close(Iterator<?> iterator) {
			closeCalled = true;
		}

		@Override
		public Iterator<Map<String, Object>> getTables(String catalog, String schema, String table) {
			getTablesCallCount++;
			return new ArrayList<>(tables).iterator();
		}

		@Override
		public Iterator<Map<String, Object>> getColumns(String catalog, String schema, String table, String column) {
			getColumnsCallCount++;
			return new ArrayList<>(columns).iterator();
		}

		@Override
		public Iterator<Map<String, Object>> getPrimaryKeys(String catalog, String schema, String name) {
			getPrimaryKeysCallCount++;
			return new ArrayList<>(primaryKeys).iterator();
		}

		@Override
		public Iterator<Map<String, Object>> getExportedKeys(String catalog, String schema, String table) {
			getExportedKeysCallCount++;
			return new ArrayList<>(exportedKeys).iterator();
		}

		@Override
		public Iterator<Map<String, Object>> getIndexInfo(String catalog, String schema, String table) {
			getIndexInfoCallCount++;
			return new ArrayList<>(indexInfo).iterator();
		}

		@Override
		public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(String catalog, String schema, String table) {
			getPkStrategyCallCount++;
			return new ArrayList<>(pkStrategies).iterator();
		}

		@Override
		public boolean needQuote(String name) {
			return "order".equalsIgnoreCase(name);
		}
	}
}
