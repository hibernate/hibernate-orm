/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Hbm2DDLExporterTaskTest {

	@Test
	public void testGetName() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		assertEquals("hbm2ddl (Generates database schema)", task.getName());
	}

	@Test
	public void testDefaultValues() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		assertTrue(task.exportToDatabase);
		assertTrue(task.scriptToConsole);
		assertFalse(task.schemaUpdate);
		assertEquals(";", task.delimiter);
		assertFalse(task.drop);
		assertTrue(task.create);
		assertFalse(task.format);
	}

	@Test
	public void testSetExport() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		task.setExport(false);
		assertFalse(task.exportToDatabase);
	}

	@Test
	public void testSetUpdate() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		task.setUpdate(true);
		assertTrue(task.schemaUpdate);
	}

	@Test
	public void testSetConsole() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		task.setConsole(false);
		assertFalse(task.scriptToConsole);
	}

	@Test
	public void testSetFormat() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		task.setFormat(true);
		assertTrue(task.format);
	}

	@Test
	public void testSetOutputFileName() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		task.setOutputFileName("schema.sql");
		assertEquals("schema.sql", task.outputFileName);
	}

	@Test
	public void testSetDrop() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		task.setDrop(true);
		assertTrue(task.drop);
	}

	@Test
	public void testSetCreate() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		task.setCreate(false);
		assertFalse(task.create);
	}

	@Test
	public void testDelimiter() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		assertEquals(";", task.getDelimiter());
		task.setDelimiter("GO");
		assertEquals("GO", task.getDelimiter());
	}

	@Test
	public void testSetHaltonerror() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		task.setHaltonerror(true);
		// no public getter, just verify no exception
	}
}
