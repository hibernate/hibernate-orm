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

public class ForeignKeyMetadataTest {

	@Test
	public void testForeignKeyMetadata() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:fk_meta_test", "sa", "");
			Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE FK_PARENT (ID INT PRIMARY KEY, NAME VARCHAR(50))");
			stmt.execute("CREATE TABLE FK_CHILD (ID INT PRIMARY KEY, PARENT_ID INT, " +
					"CONSTRAINT FK_CHILD_PARENT FOREIGN KEY (PARENT_ID) REFERENCES FK_PARENT(ID))");

			ResultSet rs = conn.getMetaData().getImportedKeys(null, "PUBLIC", "FK_CHILD");
			assertTrue(rs.next());
			ForeignKeyMetadata fkMeta = new ForeignKeyMetadata(rs);

			assertNotNull(fkMeta.getName());
			assertEquals("FK_PARENT", fkMeta.getReferencedTableName());
			assertTrue(fkMeta.toString().contains(fkMeta.getName()));

			// addReference to populate the references map
			fkMeta.addReference(rs);
			rs.close();

			stmt.execute("DROP TABLE FK_CHILD");
			stmt.execute("DROP TABLE FK_PARENT");
		}
	}
}
