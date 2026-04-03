/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HibernateToolTaskExtendedTest {

	@Test
	public void testCreateHbm2HbmXml() {
		HibernateToolTask task = new HibernateToolTask();
		ExporterTask gen = task.createHbm2HbmXml();
		assertNotNull(gen);
		assertTrue(gen instanceof Hbm2HbmXmlExporterTask);
		assertEquals(1, task.generators.size());
	}

	@Test
	public void testSetTemplatePath() {
		HibernateToolTask task = new HibernateToolTask();
		task.setTemplatePath(new org.apache.tools.ant.types.Path(null));
		assertNotNull(task.templatePath);
	}

	@Test
	public void testGetTemplatePathDefault() {
		HibernateToolTask task = new HibernateToolTask();
		task.setProject(new Project());
		assertNotNull(task.getTemplatePath());
	}

	@Test
	public void testSetClasspath() {
		HibernateToolTask task = new HibernateToolTask();
		org.apache.tools.ant.types.Path path = new org.apache.tools.ant.types.Path(null);
		task.setClasspath(path);
		assertEquals(path, task.classPath);
	}

	@Test
	public void testCreateClasspath() {
		HibernateToolTask task = new HibernateToolTask();
		task.setProject(new Project());
		org.apache.tools.ant.types.Path path = task.createClasspath();
		assertNotNull(path);
	}

	@Test
	public void testClone() throws CloneNotSupportedException {
		HibernateToolTask task = new HibernateToolTask();
		task.setDestDir(new File("/tmp/dest"));
		task.properties.put("key", "value");

		HibernateToolTask clone = (HibernateToolTask) task.clone();
		assertNotNull(clone);
		assertEquals(new File("/tmp/dest"), clone.getDestDir());
		assertEquals("value", clone.properties.getProperty("key"));
	}

	@Test
	public void testExecuteNoGeneratorsThrows() {
		HibernateToolTask task = new HibernateToolTask();
		task.setProject(new Project());
		task.createConfiguration();
		// No generators added
		assertThrows(BuildException.class, task::execute);
	}

	@Test
	public void testCreateMultipleExporters() {
		HibernateToolTask task = new HibernateToolTask();
		task.createHbm2DDL();
		task.createHbm2Java();
		task.createHbm2Doc();
		assertEquals(3, task.generators.size());
	}
}
