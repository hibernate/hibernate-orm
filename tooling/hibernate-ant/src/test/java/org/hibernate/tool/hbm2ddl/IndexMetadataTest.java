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

public class IndexMetadataTest {

	@Test
	public void testIndexMetadata() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:idx_meta_test", "sa", "");
			Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE IDX_TEST (ID INT PRIMARY KEY, NAME VARCHAR(50))");
			stmt.execute("CREATE INDEX IDX_NAME ON IDX_TEST(NAME)");

			ResultSet rs = conn.getMetaData().getIndexInfo(null, "PUBLIC", "IDX_TEST", false, true);
			IndexMetadata idx = null;
			while (rs.next()) {
				String indexName = rs.getString("INDEX_NAME");
				if ("IDX_NAME".equals(indexName)) {
					idx = new IndexMetadata(rs);
					break;
				}
			}
			rs.close();

			assertNotNull(idx);
			assertEquals("IDX_NAME", idx.getName());
			assertTrue(idx.toString().contains("IDX_NAME"));
			assertEquals(0, idx.getColumns().length);

			// Test addColumn
			ResultSet colRs = conn.getMetaData().getColumns(null, "PUBLIC", "IDX_TEST", "NAME");
			assertTrue(colRs.next());
			ColumnMetadata col = new ColumnMetadata(colRs);
			colRs.close();

			idx.addColumn(col);
			assertEquals(1, idx.getColumns().length);
			assertEquals("NAME", idx.getColumns()[0].getName());

			// addColumn with null should be a no-op
			idx.addColumn(null);
			assertEquals(1, idx.getColumns().length);

			stmt.execute("DROP TABLE IDX_TEST");
		}
	}
}
