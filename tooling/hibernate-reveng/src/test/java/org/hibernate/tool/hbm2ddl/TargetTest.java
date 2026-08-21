/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TargetTest {

	@Test
	public void testExportDoExportAndDoScript() {
		assertTrue(Target.EXPORT.doExport());
		assertFalse(Target.EXPORT.doScript());
	}

	@Test
	public void testScriptDoExportAndDoScript() {
		assertFalse(Target.SCRIPT.doExport());
		assertTrue(Target.SCRIPT.doScript());
	}

	@Test
	public void testBothDoExportAndDoScript() {
		assertTrue(Target.BOTH.doExport());
		assertTrue(Target.BOTH.doScript());
	}

	@Test
	public void testNoneDoExportAndDoScript() {
		assertFalse(Target.NONE.doExport());
		assertFalse(Target.NONE.doScript());
	}

	@Test
	public void testInterpret() {
		assertEquals(Target.BOTH, Target.interpret(true, true));
		assertEquals(Target.SCRIPT, Target.interpret(true, false));
		assertEquals(Target.EXPORT, Target.interpret(false, true));
		assertEquals(Target.NONE, Target.interpret(false, false));
	}
}
