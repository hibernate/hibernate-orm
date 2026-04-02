/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ConfigurationTaskTest {

	@Test
	public void testDefaultDescription() {
		ConfigurationTask task = new ConfigurationTask();
		assertEquals("Standard Configuration", task.getDescription());
	}

	@Test
	public void testConfigurationFile() {
		ConfigurationTask task = new ConfigurationTask();
		assertNull(task.getConfigurationFile());
		File f = new File("/tmp/hibernate.cfg.xml");
		task.setConfigurationFile(f);
		assertEquals(f, task.getConfigurationFile());
	}

	@Test
	public void testPropertyFile() {
		ConfigurationTask task = new ConfigurationTask();
		assertNull(task.getPropertyFile());
		File f = new File("/tmp/hibernate.properties");
		task.setPropertyFile(f);
		assertEquals(f, task.getPropertyFile());
	}

	@Test
	public void testEntityResolver() {
		ConfigurationTask task = new ConfigurationTask();
		assertNull(task.entityResolver);
		task.setEntityResolver("org.example.MyResolver");
		assertEquals("org.example.MyResolver", task.entityResolver);
	}

	@Test
	public void testNamingStrategy() {
		ConfigurationTask task = new ConfigurationTask();
		// setNamingStrategy just logs, should not throw
		task.setNamingStrategy("org.example.MyNamingStrategy");
	}

	@Test
	public void testClone() throws CloneNotSupportedException {
		ConfigurationTask task = new ConfigurationTask();
		File propFile = new File("/tmp/test.properties");
		task.setPropertyFile(propFile);
		task.setEntityResolver("org.example.Resolver");

		ConfigurationTask clone = (ConfigurationTask) task.clone();
		assertNotNull(clone);
		assertEquals(propFile, clone.getPropertyFile());
		assertEquals("org.example.Resolver", clone.entityResolver);
	}

	@Test
	public void testLoadPropertiesFileNull() {
		ConfigurationTask task = new ConfigurationTask();
		// propertyFile is null, should return null
		assertNull(task.loadPropertiesFile());
	}
}
