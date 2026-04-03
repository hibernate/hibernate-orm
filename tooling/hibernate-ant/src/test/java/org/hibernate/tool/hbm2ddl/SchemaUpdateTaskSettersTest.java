/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaUpdateTaskSettersTest {

	@Test
	public void testSetText() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		task.setText(false);
		// default is true, changing to false
	}

	@Test
	public void testSetQuiet() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		task.setQuiet(true);
	}

	@Test
	public void testSetConfig() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		File cfg = new File("/tmp/hibernate.cfg.xml");
		task.setConfig(cfg);
	}

	@Test
	public void testSetPropertiesNonExistent() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		task.setProject(new Project());
		assertThrows(BuildException.class,
				() -> task.setProperties(new File("/nonexistent/hibernate.properties")));
	}

	@Test
	public void testOutputFile() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		assertNull(task.getOutputFile());
		File f = new File("/tmp/update.sql");
		task.setOutputFile(f);
		assertEquals(f, task.getOutputFile());
	}

	@Test
	public void testHaltOnError() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		assertFalse(task.isHaltOnError());
		task.setHaltOnError(true);
		assertTrue(task.isHaltOnError());
	}

	@Test
	public void testDelimiter() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		assertNull(task.getDelimiter());
		task.setDelimiter(";");
		assertEquals(";", task.getDelimiter());
	}

	@Test
	public void testSetNamingStrategy() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		// deprecated, just logs
		task.setNamingStrategy("ignored");
	}

	@Test
	public void testSetImplicitNamingStrategy() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		task.setImplicitNamingStrategy("org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl");
	}

	@Test
	public void testSetPhysicalNamingStrategy() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		task.setPhysicalNamingStrategy("org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
	}
}
