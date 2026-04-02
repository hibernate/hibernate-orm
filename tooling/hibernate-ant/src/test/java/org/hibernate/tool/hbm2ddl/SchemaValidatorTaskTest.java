/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.apache.tools.ant.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class SchemaValidatorTaskTest {

	private Project antProject;

	@BeforeEach
	public void setUp() {
		antProject = new Project();
		antProject.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
		antProject.setProperty("hibernate.connection.url",
				"jdbc:h2:mem:schema_validator_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		antProject.setProperty("hibernate.connection.username", "sa");
		antProject.setProperty("hibernate.connection.password", "");
		antProject.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
	}

	@Test
	public void testExecute() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setProject(antProject);
		// With no mapping files and an empty H2 database, validation should succeed
		assertDoesNotThrow(task::execute);
	}
}
