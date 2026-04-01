/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaExportActionTest {

	@Test
	public void testActionCreate() {
		SchemaExport.Action action = SchemaExport.Action.CREATE;
		assertTrue(action.doCreate());
		assertFalse(action.doDrop());
	}

	@Test
	public void testActionDrop() {
		SchemaExport.Action action = SchemaExport.Action.DROP;
		assertFalse(action.doCreate());
		assertTrue(action.doDrop());
	}

	@Test
	public void testActionBoth() {
		SchemaExport.Action action = SchemaExport.Action.BOTH;
		assertTrue(action.doCreate());
		assertTrue(action.doDrop());
	}

	@Test
	public void testActionNone() {
		SchemaExport.Action action = SchemaExport.Action.NONE;
		assertFalse(action.doCreate());
		assertFalse(action.doDrop());
	}

	@Test
	public void testParseCommandLineOption() {
		assertEquals(SchemaExport.Action.CREATE, SchemaExport.Action.parseCommandLineOption("create"));
		assertEquals(SchemaExport.Action.DROP, SchemaExport.Action.parseCommandLineOption("drop"));
		assertEquals(SchemaExport.Action.BOTH, SchemaExport.Action.parseCommandLineOption("drop-and-create"));
		assertEquals(SchemaExport.Action.NONE, SchemaExport.Action.parseCommandLineOption("unknown"));
	}
}
