/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.dialect;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class OracleQueryExecutorTest {

	private static class StubConnection
			implements java.lang.reflect.InvocationHandler {
		final List<String> preparedSqls = new ArrayList<>();
		final StubStatement lastStatement = new StubStatement();

		@Override
		public Object invoke(Object proxy, java.lang.reflect.Method method,
							Object[] args) throws Throwable {
			if ("prepareStatement".equals(method.getName())) {
				preparedSqls.add((String) args[0]);
				return java.lang.reflect.Proxy.newProxyInstance(
						PreparedStatement.class.getClassLoader(),
						new Class[]{PreparedStatement.class},
						lastStatement);
			}
			return null;
		}

		Connection asConnection() {
			return (Connection) java.lang.reflect.Proxy.newProxyInstance(
					Connection.class.getClassLoader(),
					new Class[]{Connection.class}, this);
		}
	}

	private static class StubStatement
			implements java.lang.reflect.InvocationHandler {
		final List<String> params = new ArrayList<>();

		@Override
		public Object invoke(Object proxy, java.lang.reflect.Method method,
							Object[] args) throws Throwable {
			if ("setString".equals(method.getName())) {
				params.add(args[0] + "=" + args[1]);
				return null;
			}
			if ("executeQuery".equals(method.getName())) {
				return java.lang.reflect.Proxy.newProxyInstance(
						ResultSet.class.getClassLoader(),
						new Class[]{ResultSet.class},
						(p, m, a) -> null);
			}
			if ("close".equals(method.getName())) {
				return null;
			}
			return null;
		}
	}

	private StubConnection newStub() {
		return new StubConnection();
	}

	@Test
	void testGetTableResultSetPreparesStatements()
			throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getTableResultSet(null, null);
		assertEquals(4, stub.preparedSqls.size());
	}

	@Test
	void testGetTableResultSetWithSchemaBindsParams()
			throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getTableResultSet("HR", null);
		assertTrue(stub.lastStatement.params.contains("1=HR"));
		assertTrue(stub.lastStatement.params.contains("2=HR"));
	}

	@Test
	void testGetTableResultSetWithSchemaAndTable()
			throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getTableResultSet("HR", "EMP");
		assertTrue(stub.lastStatement.params.contains("1=HR"));
		assertTrue(stub.lastStatement.params.contains("2=EMP"));
		assertTrue(stub.lastStatement.params.contains("3=HR"));
		assertTrue(stub.lastStatement.params.contains("4=EMP"));
	}

	@Test
	void testGetIndexInfoPreparesStatements()
			throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getIndexInfoResultSet(null, null);
		assertEquals(4, stub.preparedSqls.size());
	}

	@Test
	void testGetIndexInfoWithSchemaAndTable()
			throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getIndexInfoResultSet("HR", "EMP");
		assertTrue(stub.lastStatement.params.contains("1=HR"));
		assertTrue(stub.lastStatement.params.contains("2=EMP"));
	}

	@Test
	void testGetColumnsPreparesStatements()
			throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getColumnsResultSet(null, null, null);
		assertEquals(8, stub.preparedSqls.size());
	}

	@Test
	void testGetColumnsWithAllParams() throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getColumnsResultSet("HR", "EMP", "NAME");
		assertTrue(stub.lastStatement.params.contains("1=HR"));
		assertTrue(stub.lastStatement.params.contains("2=EMP"));
		assertTrue(stub.lastStatement.params.contains("3=NAME"));
	}

	@Test
	void testGetColumnsSchemaOnly() throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getColumnsResultSet("HR", null, null);
		assertTrue(stub.lastStatement.params.contains("1=HR"));
		assertEquals(1, stub.lastStatement.params.size());
	}

	@Test
	void testGetColumnsTableAndColumn() throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getColumnsResultSet(null, "EMP", "NAME");
		assertTrue(stub.lastStatement.params.contains("1=EMP"));
		assertTrue(
				stub.lastStatement.params.contains("2=NAME"));
	}

	@Test
	void testGetPrimaryKeysPreparesStatements()
			throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getPrimaryKeysResultSet("HR", "EMP");
		assertEquals(4, stub.preparedSqls.size());
	}

	@Test
	void testGetPrimaryKeysEscapesUnderscores()
			throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getPrimaryKeysResultSet(
				"MY_SCHEMA", "MY_TABLE");
		assertTrue(stub.lastStatement.params
				.contains("1=MY\\_SCHEMA"));
		assertTrue(stub.lastStatement.params
				.contains("2=MY\\_TABLE"));
	}

	@Test
	void testGetExportedKeysPreparesStatements()
			throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getExportedKeysResultSet(null, null);
		assertEquals(4, stub.preparedSqls.size());
	}

	@Test
	void testGetExportedKeysWithSchemaAndTable()
			throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getExportedKeysResultSet("HR", "EMP");
		assertTrue(stub.lastStatement.params.contains("1=HR"));
		assertTrue(
				stub.lastStatement.params.contains("2=EMP"));
	}

	@Test
	void testCloseAllWithoutPriorUse() {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		assertDoesNotThrow(executor::closeAll);
	}

	@Test
	void testCloseAllAfterUse() throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getTableResultSet(null, null);
		assertDoesNotThrow(executor::closeAll);
	}

	@Test
	void testStatementsReusedOnSecondCall()
			throws SQLException {
		StubConnection stub = newStub();
		OracleQueryExecutor executor =
				new OracleQueryExecutor(stub::asConnection);
		executor.getTableResultSet(null, null);
		int firstCount = stub.preparedSqls.size();
		executor.getTableResultSet("HR", null);
		assertEquals(firstCount, stub.preparedSqls.size(),
				"Should not prepare new statements");
	}
}
