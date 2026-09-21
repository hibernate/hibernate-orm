/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.mapping.PhysicalTable;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.relational.naming.spi.QualifiedPhysicalName;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ForeignKeyMetadataTest {

	@Test
	public void testConstructorAndGetters() throws Exception {
		String url = "jdbc:h2:mem:fk_metadata_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
		try (var conn = DriverManager.getConnection(url, "sa", "")) {
			try (var stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE parent_t (id INT PRIMARY KEY)");
				stmt.execute("CREATE TABLE child_t (id INT PRIMARY KEY, parent_id INT, " +
						"CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES parent_t(id))");
			}
			try (var rs = conn.getMetaData().getImportedKeys(null, null, "CHILD_T")) {
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
		try (var conn = DriverManager.getConnection(url, "sa", "")) {
			try (var stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE ref_table (id INT PRIMARY KEY)");
				stmt.execute("CREATE TABLE fk_table (id INT PRIMARY KEY, ref_id INT, " +
						"CONSTRAINT fk_ref FOREIGN KEY (ref_id) REFERENCES ref_table(id))");
			}
			ForeignKeyMetadata fkMeta;
			try (var rs = conn.getMetaData().getImportedKeys(null, null, "FK_TABLE")) {
				assertTrue(rs.next());
				fkMeta = new ForeignKeyMetadata(rs);
				fkMeta.addReference(rs);
			}

			// Build a matching ForeignKey mapping object
			Table refTable = table( "REF_TABLE" );
			Column pkCol = new Column( new PhysicalName.Factory( (text, quoted) -> text ).create( "ID", false ) );
			PrimaryKey pk = new PrimaryKey(refTable);
			pk.addColumn(pkCol);
			refTable.setPrimaryKey(pk);

			Table fkTable = table( "FK_TABLE" );
			Column fkCol = new Column( new PhysicalName.Factory( (text, quoted) -> text ).create( "REF_ID", false ) );
			ForeignKey fk = new ForeignKey(fkTable);
			fk.setReferencedTable(refTable);
			fk.addColumn(fkCol);

			assertTrue(fkMeta.matches(fk));
		}
	}

	@Test
	public void testMatchesWrongTable() throws Exception {
		String url = "jdbc:h2:mem:fk_nomatch_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
		try (var conn = DriverManager.getConnection(url, "sa", "")) {
			try (var stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE tbl_a (id INT PRIMARY KEY)");
				stmt.execute("CREATE TABLE tbl_b (id INT PRIMARY KEY, a_id INT, " +
						"CONSTRAINT fk_a FOREIGN KEY (a_id) REFERENCES tbl_a(id))");
			}
			ForeignKeyMetadata fkMeta;
			try (var rs = conn.getMetaData().getImportedKeys(null, null, "TBL_B")) {
				assertTrue(rs.next());
				fkMeta = new ForeignKeyMetadata(rs);
				fkMeta.addReference(rs);
			}

			// ForeignKey pointing to a different table
			Table wrongTable = table( "WRONG_TABLE" );
			ForeignKey fk = new ForeignKey(table( "TBL_B" ));
			fk.setReferencedTable(wrongTable);
			fk.addColumn(new Column( new PhysicalName.Factory( (text, quoted) -> text ).create( "A_ID", false ) ));

			assertFalse(fkMeta.matches(fk));
		}
	}

	@Test
	public void testMatchesWrongColumnCount() throws Exception {
		String url = "jdbc:h2:mem:fk_colcount_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
		try (var conn = DriverManager.getConnection(url, "sa", "")) {
			try (var stmt = conn.createStatement()) {
				stmt.execute("CREATE TABLE cc_parent (id INT PRIMARY KEY)");
				stmt.execute("CREATE TABLE cc_child (id INT PRIMARY KEY, pid INT, " +
						"CONSTRAINT fk_cc FOREIGN KEY (pid) REFERENCES cc_parent(id))");
			}
			ForeignKeyMetadata fkMeta;
			try (var rs = conn.getMetaData().getImportedKeys(null, null, "CC_CHILD")) {
				assertTrue(rs.next());
				fkMeta = new ForeignKeyMetadata(rs);
				fkMeta.addReference(rs);
			}

			// ForeignKey with 2 columns instead of 1
			Table refTable = table( "CC_PARENT" );
			PrimaryKey pk = new PrimaryKey(refTable);
			pk.addColumn(new Column( new PhysicalName.Factory( (text, quoted) -> text ).create( "ID", false ) ));
			pk.addColumn(new Column( new PhysicalName.Factory( (text, quoted) -> text ).create( "ID2", false ) ));
			refTable.setPrimaryKey(pk);

			ForeignKey fk = new ForeignKey(table( "CC_CHILD" ));
			fk.setReferencedTable(refTable);
			fk.addColumn(new Column( new PhysicalName.Factory( (text, quoted) -> text ).create( "PID", false ) ));
			fk.addColumn(new Column( new PhysicalName.Factory( (text, quoted) -> text ).create( "PID2", false ) ));

			assertFalse(fkMeta.matches(fk));
		}
	}
	private static PhysicalTable table(String name) {
		final var factory = new PhysicalName.Factory( (text, quoted) -> text );
		return new PhysicalTable( "orm",
				new QualifiedPhysicalName( null, null, factory.create( name, false ) ), false );
	}

}
