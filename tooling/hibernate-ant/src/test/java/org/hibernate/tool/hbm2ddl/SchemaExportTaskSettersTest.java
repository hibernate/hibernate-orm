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

public class SchemaExportTaskSettersTest {

	@TempDir
	private File tempDir;

	@Test
	public void testSetPropertiesValid() throws IOException {
		SchemaExportTask task = new SchemaExportTask();
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
	public void testSetPropertiesNonExistent() {
		SchemaExportTask task = new SchemaExportTask();
		task.setProject(new Project());
		assertThrows(BuildException.class,
				() -> task.setProperties(new File("/nonexistent/hibernate.properties")));
	}

	@Test
	public void testSetConfig() {
		SchemaExportTask task = new SchemaExportTask();
		task.setConfig(new File("/tmp/hibernate.cfg.xml"));
	}

	@Test
	public void testSetQuiet() {
		SchemaExportTask task = new SchemaExportTask();
		task.setQuiet(true);
	}

	@Test
	public void testSetText() {
		SchemaExportTask task = new SchemaExportTask();
		task.setText(true);
	}

	@Test
	public void testSetDrop() {
		SchemaExportTask task = new SchemaExportTask();
		task.setDrop(true);
	}

	@Test
	public void testSetCreate() {
		SchemaExportTask task = new SchemaExportTask();
		task.setCreate(true);
	}

	@Test
	public void testSetDelimiter() {
		SchemaExportTask task = new SchemaExportTask();
		task.setDelimiter(";");
	}

	@Test
	public void testSetOutput() {
		SchemaExportTask task = new SchemaExportTask();
		task.setOutput(new File("/tmp/output.sql"));
	}

	@Test
	public void testSetHaltonerror() {
		SchemaExportTask task = new SchemaExportTask();
		task.setHaltonerror(true);
	}

	@Test
	public void testSetNamingStrategyDeprecated() {
		SchemaExportTask task = new SchemaExportTask();
		task.setNamingStrategy("org.example.Strategy");
	}

	@Test
	public void testSetImplicitNamingStrategy() {
		SchemaExportTask task = new SchemaExportTask();
		task.setImplicitNamingStrategy("org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl");
	}

	@Test
	public void testSetPhysicalNamingStrategy() {
		SchemaExportTask task = new SchemaExportTask();
		task.setPhysicalNamingStrategy("org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
	}

	@Test
	public void testAddFileset() {
		SchemaExportTask task = new SchemaExportTask();
		task.addFileset(new FileSet());
	}
}
