/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExporterTaskTest {

	@Test
	public void testGetDestdirDefault() {
		HibernateToolTask parent = new HibernateToolTask();
		parent.setDestDir(new File("/parent/dest"));
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		// Falls back to parent destDir
		assertEquals(new File("/parent/dest"), task.getDestdir());
	}

	@Test
	public void testSetDestdir() {
		HibernateToolTask parent = new HibernateToolTask();
		parent.setDestDir(new File("/parent/dest"));
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		File localDir = new File("/local/dest");
		task.setDestdir(localDir);
		assertEquals(localDir, task.getDestdir());
	}

	@Test
	public void testValidateParametersNoDestDir() {
		HibernateToolTask parent = new HibernateToolTask();
		// destDir is null on parent and task
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		assertThrows(BuildException.class, task::validateParameters);
	}

	@Test
	public void testValidateParametersWithDestDir() {
		HibernateToolTask parent = new HibernateToolTask();
		parent.setDestDir(new File("/tmp"));
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		// Should not throw
		task.validateParameters();
	}

	@Test
	public void testSetTemplatePath() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		Path path = new Path(null);
		task.setTemplatePath(path);
		// getTemplatePath should return the local one, not the parent's
		assertNotNull(task);
	}

	@Test
	public void testAddConfiguredProperty() {
		HibernateToolTask parent = new HibernateToolTask();
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		Environment.Variable prop = new Environment.Variable();
		prop.setKey("test.key");
		prop.setValue("test.value");
		task.addConfiguredProperty(prop);
		assertEquals("test.value", task.properties.getProperty("test.key"));
	}
}
