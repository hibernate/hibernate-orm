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
	public void testCreate() {
		SchemaExportTask.ExportType type = SchemaExportTask.ExportType.CREATE;
		assertTrue(type.doCreate());
		assertFalse(type.doDrop());
		assertEquals(Action.CREATE_ONLY, type.getAction());
	}

	@Test
	public void testDrop() {
		SchemaExportTask.ExportType type = SchemaExportTask.ExportType.DROP;
		assertFalse(type.doCreate());
		assertTrue(type.doDrop());
		assertEquals(Action.DROP, type.getAction());
	}

	@Test
	public void testNone() {
		SchemaExportTask.ExportType type = SchemaExportTask.ExportType.NONE;
		assertFalse(type.doCreate());
		assertFalse(type.doDrop());
		assertEquals(Action.NONE, type.getAction());
	}

	@Test
	public void testBoth() {
		SchemaExportTask.ExportType type = SchemaExportTask.ExportType.BOTH;
		assertTrue(type.doCreate());
		assertTrue(type.doDrop());
		assertEquals(Action.CREATE, type.getAction());
	}

	@Test
	public void testInterpretJustDrop() {
		assertEquals(SchemaExportTask.ExportType.DROP, SchemaExportTask.ExportType.interpret(true, false));
	}

	@Test
	public void testInterpretJustCreate() {
		assertEquals(SchemaExportTask.ExportType.CREATE, SchemaExportTask.ExportType.interpret(false, true));
	}

	@Test
	public void testInterpretBoth() {
		assertEquals(SchemaExportTask.ExportType.BOTH, SchemaExportTask.ExportType.interpret(false, false));
	}

	@Test
	public void testInterpretDropTakesPrecedence() {
		assertEquals(SchemaExportTask.ExportType.DROP, SchemaExportTask.ExportType.interpret(true, true));
	}
}
