/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
public class ForeignKeyMetadataTest {

	@Test
	public void testConstructorAndGetters() throws Exception {
		String url = "jdbc:h2:mem:fk_metadata_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
		try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE parent_t (id INT PRIMARY KEY)");
				stmt.execute("CREATE TABLE child_t (id INT PRIMARY KEY, parent_id INT, " +
						"CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES parent_t(id))");
			}
			try (ResultSet rs = conn.getMetaData().getImportedKeys(null, null, "CHILD_T")) {
				assertTrue(rs.next());
				ForeignKeyMetadata fkMeta = new ForeignKeyMetadata(rs);
				assertEquals("FK_PARENT", fkMeta.getName());
				assertEquals("PARENT_T", fkMeta.getReferencedTableName());
				assertEquals("ForeignKeyMetadata(FK_PARENT)", fkMeta.toString());
				fkMeta.addReference(rs);
			}
		}
	}

	@Test
	public void testMatchesWithPrimaryKeyReference() throws Exception {
		String url = "jdbc:h2:mem:fk_match_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
		try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE ref_table (id INT PRIMARY KEY)");
				stmt.execute("CREATE TABLE fk_table (id INT PRIMARY KEY, ref_id INT, " +
						"CONSTRAINT fk_ref FOREIGN KEY (ref_id) REFERENCES ref_table(id))");
			}
			ForeignKeyMetadata fkMeta;
			try (ResultSet rs = conn.getMetaData().getImportedKeys(null, null, "FK_TABLE")) {
				assertTrue(rs.next());
				fkMeta = new ForeignKeyMetadata(rs);
				fkMeta.addReference(rs);
			}

			// Build a matching ForeignKey mapping object
			Table refTable = new Table("orm", "REF_TABLE");
			Column pkCol = new Column("ID");
			PrimaryKey pk = new PrimaryKey(refTable);
			pk.addColumn(pkCol);
			refTable.setPrimaryKey(pk);

			Table fkTable = new Table("orm", "FK_TABLE");
			Column fkCol = new Column("REF_ID");
			ForeignKey fk = new ForeignKey();
			fk.setTable(fkTable);
			fk.setReferencedTable(refTable);
			fk.addColumn(fkCol);

			assertTrue(fkMeta.matches(fk));
		}
	}

	@Test
	public void testMatchesWrongTable() throws Exception {
		String url = "jdbc:h2:mem:fk_nomatch_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
		try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE tbl_a (id INT PRIMARY KEY)");
				stmt.execute("CREATE TABLE tbl_b (id INT PRIMARY KEY, a_id INT, " +
						"CONSTRAINT fk_a FOREIGN KEY (a_id) REFERENCES tbl_a(id))");
			}
			ForeignKeyMetadata fkMeta;
			try (ResultSet rs = conn.getMetaData().getImportedKeys(null, null, "TBL_B")) {
				assertTrue(rs.next());
				fkMeta = new ForeignKeyMetadata(rs);
				fkMeta.addReference(rs);
			}

			// ForeignKey pointing to a different table
			Table wrongTable = new Table("orm", "WRONG_TABLE");
			ForeignKey fk = new ForeignKey();
			fk.setTable(new Table("orm", "TBL_B"));
			fk.setReferencedTable(wrongTable);
			fk.addColumn(new Column("A_ID"));

			assertFalse(fkMeta.matches(fk));
		}
	}

	@Test
	public void testMatchesWrongColumnCount() throws Exception {
		String url = "jdbc:h2:mem:fk_colcount_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
		try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE cc_parent (id INT PRIMARY KEY)");
				stmt.execute("CREATE TABLE cc_child (id INT PRIMARY KEY, pid INT, " +
						"CONSTRAINT fk_cc FOREIGN KEY (pid) REFERENCES cc_parent(id))");
			}
			ForeignKeyMetadata fkMeta;
			try (ResultSet rs = conn.getMetaData().getImportedKeys(null, null, "CC_CHILD")) {
				assertTrue(rs.next());
				fkMeta = new ForeignKeyMetadata(rs);
				fkMeta.addReference(rs);
			}

			// ForeignKey with 2 columns instead of 1
			Table refTable = new Table("orm", "CC_PARENT");
			PrimaryKey pk = new PrimaryKey(refTable);
			pk.addColumn(new Column("ID"));
			pk.addColumn(new Column("ID2"));
			refTable.setPrimaryKey(pk);

			ForeignKey fk = new ForeignKey();
			fk.setTable(new Table("orm", "CC_CHILD"));
			fk.setReferencedTable(refTable);
			fk.addColumn(new Column("PID"));
			fk.addColumn(new Column("PID2"));

			assertFalse(fkMeta.matches(fk));
		}
	}
}
