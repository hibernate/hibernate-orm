/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Hbm2DDLExporterTaskTest {

	@Test
	public void testGetName() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		assertEquals("hbm2ddl (Generates database schema)", task.getName());
	}

	@Test
	public void testDefaults() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		assertTrue(task.exportToDatabase);
		assertTrue(task.scriptToConsole);
		assertFalse(task.schemaUpdate);
		assertEquals(";", task.delimiter);
		assertFalse(task.drop);
		assertTrue(task.create);
		assertFalse(task.format);
		assertNull(task.outputFileName);
	}

	@Test
	public void testSetters() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		task.setExport(false);
		assertFalse(task.exportToDatabase);
		task.setConsole(false);
		assertFalse(task.scriptToConsole);
		task.setUpdate(true);
		assertTrue(task.schemaUpdate);
		task.setFormat(true);
		assertTrue(task.format);
		task.setDrop(true);
		assertTrue(task.drop);
		task.setCreate(false);
		assertFalse(task.create);
		task.setOutputFileName("schema.sql");
		assertEquals("schema.sql", task.outputFileName);
		task.setDelimiter("GO");
		assertEquals("GO", task.getDelimiter());
		task.setHaltonerror(true);
	}
}
