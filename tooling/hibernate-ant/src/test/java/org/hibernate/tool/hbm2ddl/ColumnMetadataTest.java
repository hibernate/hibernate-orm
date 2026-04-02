/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ColumnMetadataTest {

	@Test
	public void testColumnMetadata() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:col_meta_test", "sa", "");
			Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE COL_TEST (ID INT PRIMARY KEY, NAME VARCHAR(50) NOT NULL, PRICE DECIMAL(10,2))");

			// Test VARCHAR column
			ResultSet rs = conn.getMetaData().getColumns(null, "PUBLIC", "COL_TEST", "NAME");
			assertTrue(rs.next());
			ColumnMetadata nameCol = new ColumnMetadata(rs);
			rs.close();

			assertEquals("NAME", nameCol.getName());
			assertNotNull(nameCol.getTypeName());
			assertEquals(50, nameCol.getColumnSize());
			assertEquals("NO", nameCol.getNullable());
			assertTrue(nameCol.toString().contains("NAME"));

			// Test DECIMAL column
			rs = conn.getMetaData().getColumns(null, "PUBLIC", "COL_TEST", "PRICE");
			assertTrue(rs.next());
			ColumnMetadata priceCol = new ColumnMetadata(rs);
			rs.close();

			assertEquals("PRICE", priceCol.getName());
			assertEquals(10, priceCol.getColumnSize());
			assertEquals(2, priceCol.getDecimalDigits());
			assertTrue(priceCol.getTypeCode() != 0);

			stmt.execute("DROP TABLE COL_TEST");
		}
	}
}
