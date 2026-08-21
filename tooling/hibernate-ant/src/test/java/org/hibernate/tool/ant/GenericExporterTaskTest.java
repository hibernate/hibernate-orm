/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenericExporterTaskTest {

	@Test
	public void testGetNameDefault() {
		HibernateToolTask parent = new HibernateToolTask();
		GenericExporterTask task = new GenericExporterTask(parent);
		assertEquals("generic exporter", task.getName());
	}

	@Test
	public void testGetNameWithExporterClass() {
		HibernateToolTask parent = new HibernateToolTask();
		GenericExporterTask task = new GenericExporterTask(parent);
		task.setExporterClass("org.example.MyExporter");
		String name = task.getName();
		assertTrue(name.contains("class: org.example.MyExporter"));
	}

	@Test
	public void testGetNameWithTemplate() {
		HibernateToolTask parent = new HibernateToolTask();
		GenericExporterTask task = new GenericExporterTask(parent);
		task.setTemplate("mytemplate.ftl");
		String name = task.getName();
		assertTrue(name.contains("template: mytemplate.ftl"));
	}

	@Test
	public void testGetNameWithBoth() {
		HibernateToolTask parent = new HibernateToolTask();
		GenericExporterTask task = new GenericExporterTask(parent);
		task.setExporterClass("org.example.MyExporter");
		task.setTemplate("mytemplate.ftl");
		String name = task.getName();
		assertTrue(name.contains("class: org.example.MyExporter"));
		assertTrue(name.contains("template: mytemplate.ftl"));
	}

	@Test
	public void testSetFilePattern() {
		HibernateToolTask parent = new HibernateToolTask();
		GenericExporterTask task = new GenericExporterTask(parent);
		task.setFilePattern("{package-name}/{class-name}.java");
		assertEquals("{package-name}/{class-name}.java", task.filePattern);
	}

	@Test
	public void testSetForEach() {
		HibernateToolTask parent = new HibernateToolTask();
		GenericExporterTask task = new GenericExporterTask(parent);
		task.setForEach("entity");
		assertEquals("entity", task.forEach);
	}
}
