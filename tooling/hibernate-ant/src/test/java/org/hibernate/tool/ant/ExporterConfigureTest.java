/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.Project;
import org.hibernate.boot.Metadata;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests configureExporter() and createExporter() for various ExporterTask subclasses,
 * using a stub MetadataDescriptor to avoid Hibernate bootstrapping.
 */
public class ExporterConfigureTest {

	private HibernateToolTask parent;

	/**
	 * Creates a ConfigurationTask with a pre-set stub MetadataDescriptor
	 * to avoid Hibernate bootstrapping.
	 */
	static ConfigurationTask createStubConfigurationTask() {
		ConfigurationTask task = new ConfigurationTask();
		task.metadataDescriptor = new MetadataDescriptor() {
			private final Properties props = new Properties();

			@Override
			public Metadata createMetadata() {
				return null;
			}

			@Override
			public Properties getProperties() {
				return props;
			}
		};
		return task;
	}

	@BeforeEach
	void setUp() {
		parent = new HibernateToolTask();
		Project project = new Project();
		project.init();
		parent.setProject(project);
		parent.setDestDir(new File("/tmp/dest"));
		// Install stub configuration so getMetadataDescriptor() works
		parent.configurationTask = createStubConfigurationTask();
	}

	@Test
	public void testHbm2DDLExporterConfigureExporter() {
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		task.setExport(false);
		task.setConsole(false);
		task.setUpdate(true);
		task.setDelimiter("GO");
		task.setDrop(true);
		task.setCreate(false);
		task.setFormat(true);
		task.setOutputFileName("schema.sql");
		task.setHaltonerror(true);

		Exporter exporter = task.createExporter();
		assertNotNull(exporter);
		Exporter configured = task.configureExporter(exporter);
		assertNotNull(configured);
		assertEquals(false, configured.getProperties().get(ExporterConstants.EXPORT_TO_DATABASE));
		assertEquals(false, configured.getProperties().get(ExporterConstants.EXPORT_TO_CONSOLE));
		assertEquals(true, configured.getProperties().get(ExporterConstants.SCHEMA_UPDATE));
		assertEquals("GO", configured.getProperties().get(ExporterConstants.DELIMITER));
		assertEquals(true, configured.getProperties().get(ExporterConstants.DROP_DATABASE));
		assertEquals(false, configured.getProperties().get(ExporterConstants.CREATE_DATABASE));
		assertEquals(true, configured.getProperties().get(ExporterConstants.FORMAT));
		assertEquals("schema.sql", configured.getProperties().get(ExporterConstants.OUTPUT_FILE_NAME));
		assertEquals(true, configured.getProperties().get(ExporterConstants.HALT_ON_ERROR));
	}

	@Test
	public void testHbm2DDLExporterConfigureExporterNullOutputFile() {
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		// outputFileName is null by default

		Exporter exporter = task.createExporter();
		Exporter configured = task.configureExporter(exporter);
		// When outputFileName is null, the key should be removed
		assertNull(configured.getProperties().get(ExporterConstants.OUTPUT_FILE_NAME));
	}

	@Test
	public void testGenericExporterCreateExporterDefault() {
		GenericExporterTask task = new GenericExporterTask(parent);
		// exporterClass is null -> uses ExporterType.GENERIC
		Exporter exporter = task.createExporter();
		assertNotNull(exporter);
	}

	@Test
	public void testGenericExporterCreateExporterWithClass() {
		GenericExporterTask task = new GenericExporterTask(parent);
		// Use a known exporter class
		task.setExporterClass(ExporterType.CFG.className());
		Exporter exporter = task.createExporter();
		assertNotNull(exporter);
	}

	@Test
	public void testGenericExporterConfigureExporter() {
		GenericExporterTask task = new GenericExporterTask(parent);
		task.setTemplate("mytemplate.ftl");
		task.setFilePattern("{package-name}/{class-name}.java");
		task.setForEach("entity");

		Exporter exporter = task.createExporter();
		Exporter configured = task.configureExporter(exporter);
		assertEquals("mytemplate.ftl", configured.getProperties().get(ExporterConstants.TEMPLATE_NAME));
		assertEquals("{package-name}/{class-name}.java", configured.getProperties().get(ExporterConstants.FILE_PATTERN));
		assertEquals("entity", configured.getProperties().get(ExporterConstants.FOR_EACH));
	}

	@Test
	public void testGenericExporterConfigureExporterNulls() {
		GenericExporterTask task = new GenericExporterTask(parent);
		// All fields are null by default

		Exporter exporter = task.createExporter();
		Exporter configured = task.configureExporter(exporter);
		assertNull(configured.getProperties().get(ExporterConstants.TEMPLATE_NAME));
		assertNull(configured.getProperties().get(ExporterConstants.FILE_PATTERN));
		assertNull(configured.getProperties().get(ExporterConstants.FOR_EACH));
	}

	@Test
	public void testQueryExporterConfigureExporter() {
		QueryExporterTask task = new QueryExporterTask(parent);
		task.addText("FROM Foo");
		task.setDestFile("output.txt");

		Exporter exporter = task.createExporter();
		Exporter configured = task.configureExporter(exporter);
		assertNotNull(configured.getProperties().get(ExporterConstants.QUERY_LIST));
		assertEquals("output.txt", configured.getProperties().get(ExporterConstants.OUTPUT_FILE_NAME));
	}

	@Test
	public void testQueryExporterConfigureExporterWithHql() {
		QueryExporterTask task = new QueryExporterTask(parent);
		task.setDestFile("output.txt");
		QueryExporterTask.HQL hql1 = task.createHql();
		hql1.addText("FROM Bar");
		QueryExporterTask.HQL hql2 = task.createHql();
		hql2.addText("FROM Baz");

		Exporter exporter = task.createExporter();
		Exporter configured = task.configureExporter(exporter);
		@SuppressWarnings("unchecked")
		java.util.List<String> queries = (java.util.List<String>) configured.getProperties().get(ExporterConstants.QUERY_LIST);
		assertNotNull(queries);
		assertEquals(2, queries.size());
		assertEquals("FROM Bar", queries.get(0));
		assertEquals("FROM Baz", queries.get(1));
	}

	@Test
	public void testQueryExporterIsNotEmpty() {
		QueryExporterTask task = new QueryExporterTask(parent);
		assertTrue(task.isNotEmpty("hello"));
		assertFalse(task.isNotEmpty(""));
		assertFalse(task.isNotEmpty(null));
	}

	@Test
	public void testQueryExporterGetName() {
		QueryExporterTask task = new QueryExporterTask(parent);
		assertEquals("query (Executes queries)", task.getName());
	}

	@Test
	public void testQueryExporterSetDestFile() {
		QueryExporterTask task = new QueryExporterTask(parent);
		task.setDestFile("results.csv");
		// Verify via configureExporter
		Exporter exporter = task.createExporter();
		Exporter configured = task.configureExporter(exporter);
		assertEquals("results.csv", configured.getProperties().get(ExporterConstants.OUTPUT_FILE_NAME));
	}

	@Test
	public void testQueryExporterAddTextEmpty() {
		QueryExporterTask task = new QueryExporterTask(parent);
		task.setDestFile("out.txt");
		task.addText(""); // Should be ignored
		// The main query field should remain empty
		Exporter exporter = task.createExporter();
		Exporter configured = task.configureExporter(exporter);
		@SuppressWarnings("unchecked")
		java.util.List<String> queries = (java.util.List<String>) configured.getProperties().get(ExporterConstants.QUERY_LIST);
		assertTrue(queries.isEmpty());
	}

	@Test
	public void testHqlAddTextEmpty() {
		QueryExporterTask task = new QueryExporterTask(parent);
		task.setDestFile("out.txt");
		QueryExporterTask.HQL hql = task.createHql();
		hql.addText(""); // Should be ignored
		// HQL query should remain empty
		Exporter exporter = task.createExporter();
		Exporter configured = task.configureExporter(exporter);
		@SuppressWarnings("unchecked")
		java.util.List<String> queries = (java.util.List<String>) configured.getProperties().get(ExporterConstants.QUERY_LIST);
		// The HQL has empty query, so it's not added to queryStrings
		assertTrue(queries.isEmpty());
	}

	@Test
	public void testHbm2JavaExporterCreateExporter() {
		Hbm2JavaExporterTask task = new Hbm2JavaExporterTask(parent);
		Exporter exporter = task.createExporter();
		assertNotNull(exporter);
	}

	@Test
	public void testHbm2DAOExporterCreateExporter() {
		Hbm2DAOExporterTask task = new Hbm2DAOExporterTask(parent);
		Exporter exporter = task.createExporter();
		assertNotNull(exporter);
	}

	@Test
	public void testHbm2CfgXmlExporterCreateExporter() {
		Hbm2CfgXmlExporterTask task = new Hbm2CfgXmlExporterTask(parent);
		Exporter exporter = task.createExporter();
		assertNotNull(exporter);
	}

	@Test
	public void testHbm2DocExporterCreateExporter() {
		Hbm2DocExporterTask task = new Hbm2DocExporterTask(parent);
		Exporter exporter = task.createExporter();
		assertNotNull(exporter);
	}

	@Test
	public void testHbm2HbmXmlExporterCreateExporter() {
		Hbm2HbmXmlExporterTask task = new Hbm2HbmXmlExporterTask(parent);
		Exporter exporter = task.createExporter();
		assertNotNull(exporter);
	}

	@Test
	public void testHbmLintExporterCreateExporter() {
		HbmLintExporterTask task = new HbmLintExporterTask(parent);
		Exporter exporter = task.createExporter();
		assertNotNull(exporter);
	}

	@Test
	public void testExporterTaskGetTemplatePath() {
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		// With no local template path, should return parent's
		assertNotNull(task.getTemplatePath());
	}

	@Test
	public void testExporterTaskGetTemplatePathLocal() {
		Hbm2DDLExporterTask task = new Hbm2DDLExporterTask(parent);
		org.apache.tools.ant.types.Path path = new org.apache.tools.ant.types.Path(parent.getProject());
		task.setTemplatePath(path);
		assertEquals(path, task.getTemplatePath());
	}
}
