/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseExporterTest {

	@Test
	public void testExportAndRelease() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:db_exporter_test", "sa", "")) {
			SqlExceptionHelper sqlHelper = new SqlExceptionHelper(true);
			ConnectionHelper helper = new SuppliedConnectionHelper(conn, sqlHelper);

			DatabaseExporter exporter = new DatabaseExporter(helper, sqlHelper);
			assertTrue(exporter.acceptsImportScripts());

			exporter.export("CREATE TABLE DB_EXP_TEST (ID INT PRIMARY KEY)");
			exporter.export("DROP TABLE DB_EXP_TEST");
			exporter.release();
		}
	}
}
