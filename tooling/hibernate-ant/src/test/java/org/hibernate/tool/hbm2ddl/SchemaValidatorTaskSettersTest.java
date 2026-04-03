/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SchemaValidatorTaskSettersTest {

	@TempDir
	private File tempDir;

	@Test
	public void testSetPropertiesNonExistent() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setProject(new Project());
		assertThrows(BuildException.class,
				() -> task.setProperties(new File("/nonexistent/hibernate.properties")));
	}

	@Test
	public void testSetPropertiesValid() throws IOException {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setProject(new Project());
		File propsFile = new File(tempDir, "hibernate.properties");
		Properties props = new Properties();
		props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		try (FileOutputStream fos = new FileOutputStream(propsFile)) {
			props.store(fos, null);
		}
		task.setProperties(propsFile);
	}

	@Test
	public void testSetConfigNonExistent() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setProject(new Project());
		assertThrows(BuildException.class,
				() -> task.setConfig(new File("/nonexistent/hibernate.cfg.xml")));
	}

	@Test
	public void testSetConfigValid() throws IOException {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.setProject(new Project());
		File cfgFile = new File(tempDir, "hibernate.cfg.xml");
		try (FileOutputStream fos = new FileOutputStream(cfgFile)) {
			fos.write("<hibernate-configuration/>".getBytes());
		}
		task.setConfig(cfgFile);
	}

	@Test
	public void testSetNamingStrategy() {
		SchemaValidatorTask task = new SchemaValidatorTask();
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

	@Test
	public void testAddFileset() {
		SchemaValidatorTask task = new SchemaValidatorTask();
		task.addFileset(new FileSet());
	}
}
