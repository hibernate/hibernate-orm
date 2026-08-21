/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
public class DatabaseExporterTest {

	@Test
	public void testExportAndRelease() throws Exception {
		String url = "jdbc:h2:mem:db_exporter_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
		Connection conn = DriverManager.getConnection(url, "sa", "");
		SqlExceptionHelper sqlHelper = new SqlExceptionHelper(true);
		SuppliedConnectionHelper connHelper = new SuppliedConnectionHelper(conn, sqlHelper);

		DatabaseExporter exporter = new DatabaseExporter(connHelper, sqlHelper);
		assertTrue(exporter.acceptsImportScripts());

		// Execute a DDL statement
		assertDoesNotThrow(() -> exporter.export("CREATE TABLE test_export (id INT PRIMARY KEY)"));

		// Verify table was created
		try (Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TEST_EXPORT'")) {
			rs.next();
			assertTrue(rs.getInt(1) > 0);
		}

		// Execute a statement that produces warnings (H2 doesn't easily produce warnings, but we test the path)
		assertDoesNotThrow(() -> exporter.export("CREATE TABLE test_export2 (id INT PRIMARY KEY)"));

		exporter.release();
	}
}
