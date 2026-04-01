/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.DdlExporterTest;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"HelloWorld.hbm.xml"
	};

	@TempDir
	public File outputFolder;

	private MetadataDescriptor metadataDescriptor;
	private File resourcesDir;

	@BeforeEach
	public void setUp() {
		resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
	}

	@Test
	public void testDdlExportCreateToFile() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.DDL);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		exporter.getProperties().put(ExporterConstants.OUTPUT_FILE_NAME, "schema.sql");
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_DATABASE, false);
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_CONSOLE, false);
		exporter.getProperties().put(ExporterConstants.CREATE_DATABASE, true);
		exporter.getProperties().put(ExporterConstants.DROP_DATABASE, false);
		exporter.start();

		File schemaFile = new File(outputFolder, "schema.sql");
		assertTrue(schemaFile.exists());
		assertTrue(schemaFile.length() > 0);
	}

	@Test
	public void testDdlExportDropToFile() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.DDL);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		exporter.getProperties().put(ExporterConstants.OUTPUT_FILE_NAME, "drop.sql");
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_DATABASE, false);
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_CONSOLE, false);
		exporter.getProperties().put(ExporterConstants.CREATE_DATABASE, false);
		exporter.getProperties().put(ExporterConstants.DROP_DATABASE, true);
		exporter.start();

		File dropFile = new File(outputFolder, "drop.sql");
		assertTrue(dropFile.exists());
		assertTrue(dropFile.length() > 0);
	}

	@Test
	public void testDdlExportBothToFile() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.DDL);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		exporter.getProperties().put(ExporterConstants.OUTPUT_FILE_NAME, "both.sql");
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_DATABASE, false);
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_CONSOLE, false);
		exporter.getProperties().put(ExporterConstants.CREATE_DATABASE, true);
		exporter.getProperties().put(ExporterConstants.DROP_DATABASE, true);
		exporter.start();

		File bothFile = new File(outputFolder, "both.sql");
		assertTrue(bothFile.exists());
		assertTrue(bothFile.length() > 0);
	}

	@Test
	public void testDdlExportNone() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.DDL);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		exporter.getProperties().put(ExporterConstants.OUTPUT_FILE_NAME, "none.sql");
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_DATABASE, false);
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_CONSOLE, false);
		exporter.getProperties().put(ExporterConstants.CREATE_DATABASE, false);
		exporter.getProperties().put(ExporterConstants.DROP_DATABASE, false);
		exporter.start();

		File noneFile = new File(outputFolder, "none.sql");
		// File may or may not exist but should be empty or very small
		assertFalse(noneFile.exists() && noneFile.length() > 0);
	}

	@Test
	public void testDdlExportWithDelimiter() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.DDL);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		exporter.getProperties().put(ExporterConstants.OUTPUT_FILE_NAME, "delim.sql");
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_DATABASE, false);
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_CONSOLE, false);
		exporter.getProperties().put(ExporterConstants.DELIMITER, "GO");
		exporter.start();

		File delimFile = new File(outputFolder, "delim.sql");
		assertTrue(delimFile.exists());
	}

	@Test
	public void testDdlExportFormatted() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.DDL);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		exporter.getProperties().put(ExporterConstants.OUTPUT_FILE_NAME, "formatted.sql");
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_DATABASE, false);
		exporter.getProperties().put(ExporterConstants.EXPORT_TO_CONSOLE, false);
		exporter.getProperties().put(ExporterConstants.FORMAT, true);
		exporter.start();

		File formattedFile = new File(outputFolder, "formatted.sql");
		assertTrue(formattedFile.exists());
		assertTrue(formattedFile.length() > 0);
	}
}
