/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.dialect;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class H2MetaDataDialectTest {

	private static final String JDBC_URL = "jdbc:h2:mem:h2dialect_test;DB_CLOSE_DELAY=-1";

	private H2MetaDataDialect dialect;
	private Connection sharedConnection;

	@BeforeEach
	public void setUp() throws Exception {
		sharedConnection = DriverManager.getConnection(JDBC_URL, "sa", "");

		// Create test table
		try (Statement stmt = sharedConnection.createStatement()) {
			stmt.execute("CREATE TABLE IF NOT EXISTS H2_DIALECT_TEST (ID INT PRIMARY KEY AUTO_INCREMENT, NAME VARCHAR(50))");
		}

		ConnectionProvider connectionProvider = new ConnectionProvider() {
			@Override
			public Connection getConnection() throws SQLException {
				return DriverManager.getConnection(JDBC_URL, "sa", "");
			}
			@Override
			public void closeConnection(Connection connection) throws SQLException {
				connection.close();
			}
			@Override
			public boolean supportsAggressiveRelease() {
				return false;
			}
			@Override
			public boolean isUnwrappableAs(Class<?> unwrapType) {
				return false;
			}
			@Override
			public <T> T unwrap(Class<T> unwrapType) {
				return null;
			}
		};

		dialect = new H2MetaDataDialect();
		dialect.configure(connectionProvider);
	}

	@AfterEach
	public void tearDown() throws Exception {
		dialect.close();
		try (Statement stmt = sharedConnection.createStatement()) {
			stmt.execute("DROP TABLE IF EXISTS H2_DIALECT_TEST");
		}
		sharedConnection.close();
	}

	@Test
	public void testGetTables() {
		Iterator<Map<String, Object>> tables = dialect.getTables(null, "PUBLIC", "H2_DIALECT%");
		assertNotNull(tables);
		assertTrue(tables.hasNext());
		Map<String, Object> table = tables.next();
		assertNotNull(table.get("TABLE_NAME"));
		dialect.close(tables);
	}

	@Test
	public void testGetColumns() {
		Iterator<Map<String, Object>> columns = dialect.getColumns(null, "PUBLIC", "H2_DIALECT_TEST", "%");
		assertNotNull(columns);
		assertTrue(columns.hasNext());
		Map<String, Object> column = columns.next();
		assertNotNull(column.get("COLUMN_NAME"));
		dialect.close(columns);
	}

	@Test
	public void testGetPrimaryKeys() {
		Iterator<Map<String, Object>> pks = dialect.getPrimaryKeys(null, "PUBLIC", "H2_DIALECT_TEST");
		assertNotNull(pks);
		assertTrue(pks.hasNext());
		Map<String, Object> pk = pks.next();
		assertNotNull(pk.get("COLUMN_NAME"));
		dialect.close(pks);
	}

	@Test
	public void testGetExportedKeys() {
		Iterator<Map<String, Object>> keys = dialect.getExportedKeys(null, "PUBLIC", "H2_DIALECT_TEST");
		assertNotNull(keys);
		// No foreign keys expected, just verify it doesn't crash
		assertFalse(keys.hasNext());
		dialect.close(keys);
	}

	@Test
	public void testGetIndexInfo() {
		Iterator<Map<String, Object>> indexes = dialect.getIndexInfo(null, "PUBLIC", "H2_DIALECT_TEST");
		assertNotNull(indexes);
		// Primary key creates an index
		dialect.close(indexes);
	}

	@Test
	public void testGetSuggestedPrimaryKeyStrategyName() {
		Iterator<Map<String, Object>> strategies = dialect.getSuggestedPrimaryKeyStrategyName(null, "PUBLIC", "H2_DIALECT_TEST");
		assertNotNull(strategies);
		dialect.close(strategies);
	}

	@Test
	public void testNeedQuote() {
		assertTrue(dialect.needQuote("table-name"));
		assertTrue(dialect.needQuote("schema.table"));
		assertTrue(dialect.needQuote("has space"));
		assertFalse(dialect.needQuote("NORMAL_TABLE"));
		assertFalse(dialect.needQuote(null));
	}

	@Test
	public void testCloseWithoutQuerying() {
		// Create a fresh dialect, configure it, and close without querying
		H2MetaDataDialect freshDialect = new H2MetaDataDialect();
		ConnectionProvider cp = new ConnectionProvider() {
			@Override
			public Connection getConnection() throws SQLException {
				return DriverManager.getConnection(JDBC_URL, "sa", "");
			}
			@Override
			public void closeConnection(Connection connection) throws SQLException {
				connection.close();
			}
			@Override
			public boolean supportsAggressiveRelease() { return false; }
			@Override
			public boolean isUnwrappableAs(Class<?> unwrapType) { return false; }
			@Override
			public <T> T unwrap(Class<T> unwrapType) { return null; }
		};
		freshDialect.configure(cp);
		// Close without having called any query method — exercises the connection==null branch
		freshDialect.close();
	}

	@Test
	public void testCloseIterator() {
		Iterator<Map<String, Object>> tables = dialect.getTables(null, "PUBLIC", "H2_DIALECT%");
		assertNotNull(tables);
		// Consume the iterator
		while (tables.hasNext()) {
			tables.next();
		}
		// close(Iterator) should close the underlying ResultSetIterator
		dialect.close(tables);
	}

	@Test
	public void testCloseNonResultSetIterator() {
		// Passing a non-ResultSetIterator should be a no-op
		Iterator<Map<String, Object>> plainIterator = java.util.Collections.<Map<String, Object>>emptyList().iterator();
		dialect.close(plainIterator);
	}
}
