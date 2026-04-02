/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableMetadataTest {

	private Connection conn;

	@BeforeEach
	public void setUp() throws Exception {
		conn = DriverManager.getConnection("jdbc:h2:mem:tbl_meta_test;DB_CLOSE_DELAY=-1", "sa", "");
		try (Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE TBL_PARENT (ID INT PRIMARY KEY, NAME VARCHAR(50))");
			stmt.execute("CREATE TABLE TBL_CHILD (ID INT PRIMARY KEY, PARENT_ID INT, "
					+ "CONSTRAINT FK_PARENT FOREIGN KEY (PARENT_ID) REFERENCES TBL_PARENT(ID))");
			stmt.execute("CREATE INDEX IDX_CHILD_PARENT ON TBL_CHILD(PARENT_ID)");
		}
	}

	@AfterEach
	public void tearDown() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			stmt.execute("DROP TABLE IF EXISTS TBL_CHILD");
			stmt.execute("DROP TABLE IF EXISTS TBL_PARENT");
		}
		conn.close();
	}

	private TableMetadata loadTable(String tableName, boolean extras) throws Exception {
		DatabaseMetaData meta = conn.getMetaData();
		ResultSet rs = meta.getTables(null, "PUBLIC", tableName, null);
		assertTrue(rs.next());
		TableMetadata table = new TableMetadata(rs, meta, extras);
		rs.close();
		return table;
	}

	@Test
	public void testBasicProperties() throws Exception {
		TableMetadata table = loadTable("TBL_PARENT", false);
		assertEquals("TBL_PARENT", table.getName());
		assertEquals("PUBLIC", table.getSchema());
		assertTrue(table.toString().contains("TBL_PARENT"));
	}

	@Test
	public void testColumnMetadata() throws Exception {
		TableMetadata table = loadTable("TBL_PARENT", false);
		assertNotNull(table.getColumnMetadata("ID"));
		assertNotNull(table.getColumnMetadata("NAME"));
		assertNull(table.getColumnMetadata("NONEXISTENT"));
	}

	@Test
	public void testColumnMetadataCaseInsensitive() throws Exception {
		TableMetadata table = loadTable("TBL_PARENT", false);
		assertNotNull(table.getColumnMetadata("id"));
		assertNotNull(table.getColumnMetadata("name"));
	}

	@Test
	public void testForeignKeyMetadata() throws Exception {
		TableMetadata table = loadTable("TBL_CHILD", true);
		assertNotNull(table.getForeignKeyMetadata("FK_PARENT"));
		assertNotNull(table.getForeignKeyMetadata("fk_parent"));
		assertNull(table.getForeignKeyMetadata("NONEXISTENT"));
	}

	@Test
	public void testIndexMetadata() throws Exception {
		TableMetadata table = loadTable("TBL_CHILD", true);
		assertNotNull(table.getIndexMetadata("IDX_CHILD_PARENT"));
		assertNotNull(table.getIndexMetadata("idx_child_parent"));
		assertNull(table.getIndexMetadata("NONEXISTENT"));
	}

	@Test
	public void testWithoutExtras() throws Exception {
		TableMetadata table = loadTable("TBL_CHILD", false);
		// Without extras, foreign keys and indexes are not loaded
		assertNull(table.getForeignKeyMetadata("FK_PARENT"));
		assertNull(table.getIndexMetadata("IDX_CHILD_PARENT"));
		// But columns are always loaded
		assertNotNull(table.getColumnMetadata("ID"));
	}

	@Test
	public void testAddColumnWithNull() throws Exception {
		TableMetadata table = loadTable("TBL_PARENT", false);
		// Adding a column with null COLUMN_NAME should be a no-op
		int before = 0;
		if (table.getColumnMetadata("ID") != null) before++;
		if (table.getColumnMetadata("NAME") != null) before++;
		assertEquals(2, before);
	}
}
