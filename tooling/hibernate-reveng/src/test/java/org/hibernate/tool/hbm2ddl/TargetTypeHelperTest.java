/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TargetTypeHelperTest {

	@Test
	public void testParseLegacyScriptAndExport() {
		EnumSet<TargetType> result = TargetTypeHelper.parseLegacyCommandLineOptions(true, true, null);
		assertTrue(result.contains(TargetType.STDOUT));
		assertTrue(result.contains(TargetType.DATABASE));
		assertFalse(result.contains(TargetType.SCRIPT));
	}

	@Test
	public void testParseLegacyScriptOnly() {
		EnumSet<TargetType> result = TargetTypeHelper.parseLegacyCommandLineOptions(true, false, null);
		assertTrue(result.contains(TargetType.STDOUT));
		assertFalse(result.contains(TargetType.DATABASE));
	}

	@Test
	public void testParseLegacyExportOnly() {
		EnumSet<TargetType> result = TargetTypeHelper.parseLegacyCommandLineOptions(false, true, null);
		assertFalse(result.contains(TargetType.STDOUT));
		assertTrue(result.contains(TargetType.DATABASE));
	}

	@Test
	public void testParseLegacyWithOutputFile() {
		EnumSet<TargetType> result = TargetTypeHelper.parseLegacyCommandLineOptions(true, true, "output.sql");
		assertTrue(result.contains(TargetType.SCRIPT));
		assertTrue(result.contains(TargetType.STDOUT));
		assertTrue(result.contains(TargetType.DATABASE));
	}

	@Test
	public void testParseLegacyQuietNoExport() {
		EnumSet<TargetType> result = TargetTypeHelper.parseLegacyCommandLineOptions(false, false, null);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testParseCommandLineDatabase() {
		EnumSet<TargetType> result = TargetTypeHelper.parseCommandLineOptions("database");
		assertEquals(1, result.size());
		assertTrue(result.contains(TargetType.DATABASE));
	}

	@Test
	public void testParseCommandLineStdout() {
		EnumSet<TargetType> result = TargetTypeHelper.parseCommandLineOptions("stdout");
		assertEquals(1, result.size());
		assertTrue(result.contains(TargetType.STDOUT));
	}

	@Test
	public void testParseCommandLineScript() {
		EnumSet<TargetType> result = TargetTypeHelper.parseCommandLineOptions("script");
		assertEquals(1, result.size());
		assertTrue(result.contains(TargetType.SCRIPT));
	}

	@Test
	public void testParseCommandLineMultiple() {
		EnumSet<TargetType> result = TargetTypeHelper.parseCommandLineOptions("database,stdout,script");
		assertEquals(3, result.size());
		assertTrue(result.contains(TargetType.DATABASE));
		assertTrue(result.contains(TargetType.STDOUT));
		assertTrue(result.contains(TargetType.SCRIPT));
	}

	@Test
	public void testParseCommandLineNone() {
		EnumSet<TargetType> result = TargetTypeHelper.parseCommandLineOptions("none");
		assertTrue(result.isEmpty());
	}

	@Test
	public void testParseCommandLineInvalid() {
		assertThrows(IllegalArgumentException.class,
				() -> TargetTypeHelper.parseCommandLineOptions("invalid"));
	}
}
