/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaUpdateCommandTest {

	@Test
	public void testGetters() {
		SchemaUpdateCommand cmd = new SchemaUpdateCommand("CREATE TABLE foo (id INT)", false);
		assertEquals("CREATE TABLE foo (id INT)", cmd.getSql());
		assertFalse(cmd.isQuiet());
	}

	@Test
	public void testQuietMode() {
		SchemaUpdateCommand cmd = new SchemaUpdateCommand("DROP TABLE bar", true);
		assertEquals("DROP TABLE bar", cmd.getSql());
		assertTrue(cmd.isQuiet());
	}
}
