/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaExportTypeTest {

	@Test
	public void testTypeCreate() {
		SchemaExport.Type type = SchemaExport.Type.CREATE;
		assertTrue(type.doCreate());
		assertFalse(type.doDrop());
	}

	@Test
	public void testTypeDrop() {
		SchemaExport.Type type = SchemaExport.Type.DROP;
		assertFalse(type.doCreate());
		assertTrue(type.doDrop());
	}

	@Test
	public void testTypeBoth() {
		SchemaExport.Type type = SchemaExport.Type.BOTH;
		assertTrue(type.doCreate());
		assertTrue(type.doDrop());
	}

	@Test
	public void testTypeNone() {
		SchemaExport.Type type = SchemaExport.Type.NONE;
		assertFalse(type.doCreate());
		assertFalse(type.doDrop());
	}

	@Test
	public void testSchemaExportSetters() {
		SchemaExport export = new SchemaExport();
		SchemaExport result = export.setOutputFile("output.sql");
		assertNotNull(result);

		result = export.setDelimiter(";");
		assertNotNull(result);

		result = export.setFormat(true);
		assertNotNull(result);

		result = export.setImportFiles("import.sql");
		assertNotNull(result);

		result = export.setOverrideOutputFileContent();
		assertNotNull(result);

		result = export.setHaltOnError(true);
		assertNotNull(result);

		result = export.setManageNamespaces(true);
		assertNotNull(result);
	}
}
