/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.tool.schema.Action;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaExportTaskExportTypeTest {

	@Test
	public void testInterpretDropOnly() {
		SchemaExportTask.ExportType type = SchemaExportTask.ExportType.interpret(true, false);
		assertEquals(SchemaExportTask.ExportType.DROP, type);
		assertTrue(type.doDrop());
		assertFalse(type.doCreate());
		assertEquals(Action.DROP, type.getAction());
	}

	@Test
	public void testInterpretCreateOnly() {
		SchemaExportTask.ExportType type = SchemaExportTask.ExportType.interpret(false, true);
		assertEquals(SchemaExportTask.ExportType.CREATE, type);
		assertFalse(type.doDrop());
		assertTrue(type.doCreate());
		assertEquals(Action.CREATE_ONLY, type.getAction());
	}

	@Test
	public void testInterpretBoth() {
		SchemaExportTask.ExportType type = SchemaExportTask.ExportType.interpret(false, false);
		assertEquals(SchemaExportTask.ExportType.BOTH, type);
		assertTrue(type.doDrop());
		assertTrue(type.doCreate());
		assertEquals(Action.CREATE, type.getAction());
	}

	@Test
	public void testNoneAction() {
		assertEquals(Action.NONE, SchemaExportTask.ExportType.NONE.getAction());
		assertFalse(SchemaExportTask.ExportType.NONE.doCreate());
		assertFalse(SchemaExportTask.ExportType.NONE.doDrop());
	}

	@Test
	public void testInterpretDropTakesPrecedence() {
		// When both drop and create are true, drop takes precedence
		SchemaExportTask.ExportType type = SchemaExportTask.ExportType.interpret(true, true);
		assertEquals(SchemaExportTask.ExportType.DROP, type);
	}
}
