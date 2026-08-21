/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.apache.tools.ant.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaUpdateTaskTest {

	@TempDir
	private File tempDir;

	private Project antProject;

	@BeforeEach
	public void setUp() {
		antProject = new Project();
		antProject.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
		antProject.setProperty("hibernate.connection.url",
				"jdbc:h2:mem:schema_update_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		antProject.setProperty("hibernate.connection.username", "sa");
		antProject.setProperty("hibernate.connection.password", "");
		antProject.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
	}

	@Test
	public void testExecuteTextOnly() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		task.setProject(antProject);
		task.setText(true);
		task.setQuiet(true);
		File output = new File(tempDir, "update.sql");
		task.setOutputFile(output);
		task.setDelimiter(";");
		assertDoesNotThrow(task::execute);
	}

	@Test
	public void testGettersSetters() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		task.setProject(antProject);

		File output = new File(tempDir, "out.sql");
		task.setOutputFile(output);
		assertEquals(output, task.getOutputFile());

		task.setHaltOnError(true);
		assertTrue(task.isHaltOnError());
		task.setHaltOnError(false);
		assertFalse(task.isHaltOnError());

		task.setDelimiter(";;");
		assertEquals(";;", task.getDelimiter());
	}
}
