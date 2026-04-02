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
		task.setExporterClass("com.example.MyExporter");
		assertTrue(task.getName().contains("com.example.MyExporter"));
	}

	@Test
	public void testGetNameWithTemplate() {
		HibernateToolTask parent = new HibernateToolTask();
		GenericExporterTask task = new GenericExporterTask(parent);
		task.setTemplate("my/template.ftl");
		assertTrue(task.getName().contains("my/template.ftl"));
	}

	@Test
	public void testSetters() {
		HibernateToolTask parent = new HibernateToolTask();
		GenericExporterTask task = new GenericExporterTask(parent);
		task.setFilePattern("{package-name}/{class-name}.java");
		assertEquals("{package-name}/{class-name}.java", task.filePattern);
		task.setForEach("entity");
		assertEquals("entity", task.forEach);
		task.setTemplate("pojo/Pojo.ftl");
		assertEquals("pojo/Pojo.ftl", task.templateName);
		task.setExporterClass("com.example.MyExporter");
		assertEquals("com.example.MyExporter", task.exporterClass);
	}
}
