/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.dialect;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResultSetIteratorTest {

	@Test
	public void testIterateOverResultSet() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:rsi_test1", "sa", "");
			Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE RSI_TEST (ID INT, NAME VARCHAR(50))");
			stmt.execute("INSERT INTO RSI_TEST VALUES (1, 'Alice')");
			stmt.execute("INSERT INTO RSI_TEST VALUES (2, 'Bob')");

			Statement queryStmt = conn.createStatement();
			ResultSet rs = queryStmt.executeQuery("SELECT ID, NAME FROM RSI_TEST ORDER BY ID");

			ResultSetIterator iter = new ResultSetIterator(queryStmt, rs) {
				final Map<String, Object> row = new HashMap<>();
				@Override
				protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
					row.clear();
					row.put("ID", rs.getInt("ID"));
					row.put("NAME", rs.getString("NAME"));
					return row;
				}
				@Override
				protected Throwable handleSQLException(SQLException e) {
					throw new RuntimeException(e);
				}
			};

			assertTrue(iter.hasNext());
			Map<String, Object> first = iter.next();
			assertEquals(1, first.get("ID"));
			assertEquals("Alice", first.get("NAME"));

			assertTrue(iter.hasNext());
			Map<String, Object> second = iter.next();
			assertEquals(2, second.get("ID"));
			assertEquals("Bob", second.get("NAME"));

			assertFalse(iter.hasNext());
			iter.close();

			stmt.execute("DROP TABLE RSI_TEST");
		}
	}

	@Test
	public void testEmptyResultSet() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:rsi_test2", "sa", "");
			Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE RSI_EMPTY (ID INT)");

			Statement queryStmt = conn.createStatement();
			ResultSet rs = queryStmt.executeQuery("SELECT ID FROM RSI_EMPTY");

			ResultSetIterator iter = new ResultSetIterator(queryStmt, rs) {
				@Override
				protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
					Map<String, Object> row = new HashMap<>();
					row.put("ID", rs.getInt("ID"));
					return row;
				}
				@Override
				protected Throwable handleSQLException(SQLException e) {
					throw new RuntimeException(e);
				}
			};

			assertFalse(iter.hasNext());
			assertThrows(NoSuchElementException.class, iter::next);
			iter.close();

			stmt.execute("DROP TABLE RSI_EMPTY");
		}
	}

	@Test
	public void testRemoveThrows() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:rsi_test3", "sa", "");
			Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE RSI_REM (ID INT)");

			Statement queryStmt = conn.createStatement();
			ResultSet rs = queryStmt.executeQuery("SELECT ID FROM RSI_REM");

			ResultSetIterator iter = new ResultSetIterator(queryStmt, rs) {
				@Override
				protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
					return new HashMap<>();
				}
				@Override
				protected Throwable handleSQLException(SQLException e) {
					throw new RuntimeException(e);
				}
			};

			assertThrows(UnsupportedOperationException.class, iter::remove);
			iter.close();

			stmt.execute("DROP TABLE RSI_REM");
		}
	}

	@Test
	public void testHasNextAfterClosedResultSet() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:rsi_test5", "sa", "");
			Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE RSI_CLOSED (ID INT)");
			stmt.execute("INSERT INTO RSI_CLOSED VALUES (1)");

			Statement queryStmt = conn.createStatement();
			ResultSet rs = queryStmt.executeQuery("SELECT ID FROM RSI_CLOSED");

			ResultSetIterator iter = new ResultSetIterator(queryStmt, rs) {
				@Override
				protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
					Map<String, Object> row = new HashMap<>();
					row.put("ID", rs.getInt("ID"));
					return row;
				}
				@Override
				protected Throwable handleSQLException(SQLException e) {
					return e;
				}
			};

			// Close the underlying result set to force SQLException on advance
			rs.close();
			assertFalse(iter.hasNext());

			stmt.execute("DROP TABLE RSI_CLOSED");
		}
	}

	@Test
	public void testNextAfterClosedResultSet() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:rsi_test6", "sa", "");
			Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE RSI_CLOSED2 (ID INT)");
			stmt.execute("INSERT INTO RSI_CLOSED2 VALUES (1)");

			Statement queryStmt = conn.createStatement();
			ResultSet rs = queryStmt.executeQuery("SELECT ID FROM RSI_CLOSED2");

			ResultSetIterator iter = new ResultSetIterator(queryStmt, rs) {
				@Override
				protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
					Map<String, Object> row = new HashMap<>();
					row.put("ID", rs.getInt("ID"));
					return row;
				}
				@Override
				protected Throwable handleSQLException(SQLException e) {
					return e;
				}
			};

			// Close result set to force SQLException on next()
			rs.close();
			assertThrows(NoSuchElementException.class, iter::next);

			stmt.execute("DROP TABLE RSI_CLOSED2");
		}
	}

	@Test
	public void testConstructorWithoutStatement() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:rsi_test4", "sa", "");
			Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE RSI_NOSTMT (ID INT)");
			stmt.execute("INSERT INTO RSI_NOSTMT VALUES (1)");

			ResultSet rs = stmt.executeQuery("SELECT ID FROM RSI_NOSTMT");

			ResultSetIterator iter = new ResultSetIterator(rs) {
				@Override
				protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
					Map<String, Object> row = new HashMap<>();
					row.put("ID", rs.getInt("ID"));
					return row;
				}
				@Override
				protected Throwable handleSQLException(SQLException e) {
					throw new RuntimeException(e);
				}
			};

			assertTrue(iter.hasNext());
			iter.next();
			assertFalse(iter.hasNext());
			iter.close();

			stmt.execute("DROP TABLE RSI_NOSTMT");
		}
	}
}
