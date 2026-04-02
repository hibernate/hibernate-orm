/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HibernateToolTaskTest {

	@Test
	public void testDestDir() {
		HibernateToolTask task = new HibernateToolTask();
		assertNull(task.getDestDir());
		File dir = new File("/tmp/test");
		task.setDestDir(dir);
		assertEquals(dir, task.getDestDir());
	}

	@Test
	public void testCreateConfiguration() {
		HibernateToolTask task = new HibernateToolTask();
		ConfigurationTask cfg = task.createConfiguration();
		assertNotNull(cfg);
	}

	@Test
	public void testCreateConfigurationTwiceThrows() {
		HibernateToolTask task = new HibernateToolTask();
		task.createConfiguration();
		assertThrows(BuildException.class, task::createConfiguration);
	}

	@Test
	public void testCreateJDBCConfiguration() {
		HibernateToolTask task = new HibernateToolTask();
		JDBCConfigurationTask cfg = task.createJDBCConfiguration();
		assertNotNull(cfg);
		assertTrue(cfg instanceof JDBCConfigurationTask);
	}

	@Test
	public void testCreateJpaConfiguration() {
		HibernateToolTask task = new HibernateToolTask();
		JPAConfigurationTask cfg = task.createJpaConfiguration();
		assertNotNull(cfg);
	}

	@Test
	public void testCreateHbm2DDL() {
		HibernateToolTask task = new HibernateToolTask();
		ExporterTask gen = task.createHbm2DDL();
		assertNotNull(gen);
		assertTrue(gen instanceof Hbm2DDLExporterTask);
		assertEquals(1, task.generators.size());
	}

	@Test
	public void testCreateHbmTemplate() {
		HibernateToolTask task = new HibernateToolTask();
		ExporterTask gen = task.createHbmTemplate();
		assertNotNull(gen);
		assertTrue(gen instanceof GenericExporterTask);
	}

	@Test
	public void testCreateHbm2CfgXml() {
		HibernateToolTask task = new HibernateToolTask();
		ExporterTask gen = task.createHbm2CfgXml();
		assertNotNull(gen);
		assertTrue(gen instanceof Hbm2CfgXmlExporterTask);
	}

	@Test
	public void testCreateHbm2Java() {
		HibernateToolTask task = new HibernateToolTask();
		ExporterTask gen = task.createHbm2Java();
		assertNotNull(gen);
		assertTrue(gen instanceof Hbm2JavaExporterTask);
	}

	@Test
	public void testCreateHbm2Doc() {
		HibernateToolTask task = new HibernateToolTask();
		ExporterTask gen = task.createHbm2Doc();
		assertNotNull(gen);
		assertTrue(gen instanceof Hbm2DocExporterTask);
	}

	@Test
	public void testCreateHbm2DAO() {
		HibernateToolTask task = new HibernateToolTask();
		ExporterTask gen = task.createHbm2DAO();
		assertNotNull(gen);
		assertTrue(gen instanceof Hbm2DAOExporterTask);
	}

	@Test
	public void testCreateQuery() {
		HibernateToolTask task = new HibernateToolTask();
		QueryExporterTask gen = task.createQuery();
		assertNotNull(gen);
		assertEquals(1, task.generators.size());
	}

	@Test
	public void testCreateHbmLint() {
		HibernateToolTask task = new HibernateToolTask();
		HbmLintExporterTask gen = task.createHbmLint();
		assertNotNull(gen);
	}

	@Test
	public void testAddConfiguredProperty() {
		HibernateToolTask task = new HibernateToolTask();
		Environment.Variable prop = new Environment.Variable();
		prop.setKey("test.key");
		prop.setValue("test.value");
		task.addConfiguredProperty(prop);
		assertEquals("test.value", task.properties.getProperty("test.key"));
	}

	@Test
	public void testAddConfiguredPropertyNullKey() {
		HibernateToolTask task = new HibernateToolTask();
		Environment.Variable prop = new Environment.Variable();
		prop.setValue("value");
		// null key should be ignored
		task.addConfiguredProperty(prop);
		assertTrue(task.properties.isEmpty());
	}

	@Test
	public void testAddConfiguredPropertyNullValue() {
		HibernateToolTask task = new HibernateToolTask();
		Environment.Variable prop = new Environment.Variable();
		prop.setKey("key");
		// null value should be ignored
		task.addConfiguredProperty(prop);
		assertNull(task.properties.getProperty("key"));
	}

	@Test
	public void testExecuteNoConfigurationThrows() {
		HibernateToolTask task = new HibernateToolTask();
		assertThrows(BuildException.class, task::execute);
	}
}
