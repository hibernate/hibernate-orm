/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SchemaExportTaskTest {

	@TempDir
	private File tempDir;

	private Project antProject;

	@BeforeEach
	public void setUp() {
		antProject = new Project();
		antProject.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
		antProject.setProperty("hibernate.connection.url",
				"jdbc:h2:mem:schema_export_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		antProject.setProperty("hibernate.connection.username", "sa");
		antProject.setProperty("hibernate.connection.password", "");
		antProject.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
	}

	@Test
	public void testExecuteTextOnlyCreate() {
		SchemaExportTask task = new SchemaExportTask();
		task.setProject(antProject);
		task.setText(true);
		task.setQuiet(true);
		task.setCreate(true);
		task.setDelimiter(";");
		assertDoesNotThrow(task::execute);
	}

	@Test
	public void testExecuteTextOnlyDrop() {
		SchemaExportTask task = new SchemaExportTask();
		task.setProject(antProject);
		task.setText(true);
		task.setQuiet(true);
		task.setDrop(true);
		task.setDelimiter(";");
		assertDoesNotThrow(task::execute);
	}

	@Test
	public void testExecuteWithOutputFile() {
		SchemaExportTask task = new SchemaExportTask();
		task.setProject(antProject);
		task.setText(true);
		task.setQuiet(true);
		task.setDelimiter(";");
		task.setCreate(true);
		File output = new File(tempDir, "schema.sql");
		task.setOutput(output);
		assertDoesNotThrow(task::execute);
	}

	@Test
	public void testExecuteExportToDatabase() {
		SchemaExportTask task = new SchemaExportTask();
		task.setProject(antProject);
		task.setQuiet(true);
		task.setCreate(true);
		task.setDelimiter(";");
		assertDoesNotThrow(task::execute);
	}

	@Test
	public void testSetPropertiesNonExistent() {
		SchemaExportTask task = new SchemaExportTask();
		task.setProject(antProject);
		assertThrows(BuildException.class,
				() -> task.setProperties(new File("/nonexistent/hibernate.properties")));
	}

	@Test
	public void testSetters() {
		SchemaExportTask task = new SchemaExportTask();
		task.setProject(antProject);
		task.setConfig(new File("/tmp/hibernate.cfg.xml"));
		task.setQuiet(true);
		task.setText(true);
		task.setDrop(true);
		task.setCreate(true);
		task.setDelimiter(";");
		task.setHaltonerror(true);
		task.setImplicitNamingStrategy("org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl");
		task.setPhysicalNamingStrategy("org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
		// setNamingStrategy is deprecated, just logs a warning
		task.setNamingStrategy("ignored");
	}
}
