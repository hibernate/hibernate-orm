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

public class SchemaValidatorTaskSettersTest {

	@Test
	public void testSetPropertiesNonExistent() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setProject(new Project());
		assertThrows(BuildException.class,
				() -> task.setProperties(new File("/nonexistent/hibernate.properties")));
	}

	@Test
	public void testSetConfigNonExistent() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setProject(new Project());
		assertThrows(BuildException.class,
				() -> task.setConfig(new File("/nonexistent/hibernate.cfg.xml")));
	}

	@Test
	public void testSetNamingStrategy() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		// deprecated, just logs
		task.setNamingStrategy("ignored");
	}

	@Test
	public void testSetImplicitNamingStrategy() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setImplicitNamingStrategy("org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl");
	}

	@Test
	public void testSetPhysicalNamingStrategy() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setPhysicalNamingStrategy("org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
	}
}
