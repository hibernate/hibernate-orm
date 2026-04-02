/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for SchemaUpdateTask and SchemaValidatorTask setters/configuration.
 */
public class SchemaExportTaskSettersTest {

	@Test
	public void testSchemaUpdateTaskSetPropertiesNonExistent() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		task.setProject(new Project());
		assertThrows(BuildException.class,
				() -> task.setProperties(new File("/nonexistent/hibernate.properties")));
	}

	@Test
	public void testSchemaUpdateTaskSetters() {
		SchemaUpdateTask task = new SchemaUpdateTask();
		task.setProject(new Project());
		task.setConfig(new File("/tmp/hibernate.cfg.xml"));
		task.setQuiet(true);
		task.setText(true);
		task.setHaltOnError(true);
		task.setDelimiter(";");
		task.setOutputFile(new File("/tmp/output.sql"));
		task.setImplicitNamingStrategy("org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl");
		task.setPhysicalNamingStrategy("org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
		task.setNamingStrategy("ignored");
	}

	@Test
	public void testSchemaValidatorTaskSetPropertiesNonExistent() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setProject(new Project());
		assertThrows(BuildException.class,
				() -> task.setProperties(new File("/nonexistent/hibernate.properties")));
	}

	@Test
	public void testSchemaValidatorTaskSetConfigNonExistent() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setProject(new Project());
		assertThrows(BuildException.class,
				() -> task.setConfig(new File("/nonexistent/hibernate.cfg.xml")));
	}

	@Test
	public void testSchemaValidatorTaskSetters() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setProject(new Project());
		task.setImplicitNamingStrategy("org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl");
		task.setPhysicalNamingStrategy("org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
		task.setNamingStrategy("ignored");
	}
}
