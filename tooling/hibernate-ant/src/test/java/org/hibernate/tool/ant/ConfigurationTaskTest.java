/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigurationTaskTest {

	@TempDir
	private File tempDir;

	@Test
	public void testDefaultDescription() {
		ConfigurationTask task = new ConfigurationTask();
		assertEquals("Standard Configuration", task.getDescription());
	}

	@Test
	public void testSetConfigurationFile() {
		ConfigurationTask task = new ConfigurationTask();
		assertNull(task.getConfigurationFile());
		File f = new File("/tmp/cfg.xml");
		task.setConfigurationFile(f);
		assertEquals(f, task.getConfigurationFile());
	}

	@Test
	public void testSetPropertyFile() {
		ConfigurationTask task = new ConfigurationTask();
		assertNull(task.getPropertyFile());
		File f = new File("/tmp/hibernate.properties");
		task.setPropertyFile(f);
		assertEquals(f, task.getPropertyFile());
	}

	@Test
	public void testSetEntityResolver() {
		ConfigurationTask task = new ConfigurationTask();
		task.setEntityResolver("org.example.Resolver");
		assertEquals("org.example.Resolver", task.entityResolver);
	}

	@Test
	public void testSetNamingStrategy() {
		ConfigurationTask task = new ConfigurationTask();
		// Should just log a warning, not throw
		task.setNamingStrategy("some.Strategy");
	}

	@Test
	public void testLoadPropertiesFileNull() {
		ConfigurationTask task = new ConfigurationTask();
		// When propertyFile is null, loadPropertiesFile returns null
		assertNull(task.loadPropertiesFile());
	}

	@Test
	public void testLoadPropertiesFileValid() throws IOException {
		ConfigurationTask task = new ConfigurationTask();
		File propsFile = new File(tempDir, "hibernate.properties");
		Properties props = new Properties();
		props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		props.setProperty("hibernate.connection.url", "jdbc:h2:mem:test");
		try (FileOutputStream fos = new FileOutputStream(propsFile)) {
			props.store(fos, null);
		}
		task.setPropertyFile(propsFile);
		Properties loaded = task.loadPropertiesFile();
		assertNotNull(loaded);
		assertEquals("org.hibernate.dialect.H2Dialect", loaded.getProperty("hibernate.dialect"));
		assertEquals("jdbc:h2:mem:test", loaded.getProperty("hibernate.connection.url"));
	}

	@Test
	public void testLoadPropertiesFileNotFound() {
		ConfigurationTask task = new ConfigurationTask();
		task.setPropertyFile(new File("/nonexistent/hibernate.properties"));
		assertThrows(BuildException.class, task::loadPropertiesFile);
	}

	@Test
	public void testClone() throws CloneNotSupportedException {
		ConfigurationTask task = new ConfigurationTask();
		File cfg = new File("/tmp/cfg.xml");
		File prop = new File("/tmp/hibernate.properties");
		task.setConfigurationFile(cfg);
		task.setPropertyFile(prop);
		task.setEntityResolver("org.example.Resolver");
		ConfigurationTask clone = (ConfigurationTask) task.clone();
		assertNotNull(clone);
		assertEquals(prop, clone.getPropertyFile());
		assertEquals("org.example.Resolver", clone.entityResolver);
	}

	@Test
	public void testGetFilesEmpty() {
		ConfigurationTask task = new ConfigurationTask();
		// With no file sets and no project, getFiles should return empty array
		// (fileSets is empty, so loop body never executes)
		File[] files = task.getFiles();
		assertNotNull(files);
		assertEquals(0, files.length);
	}
}
