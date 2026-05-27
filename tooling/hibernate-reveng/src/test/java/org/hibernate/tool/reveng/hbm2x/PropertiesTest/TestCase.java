/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.PropertiesTest;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.export.common.DefaultArtifactCollector;
import org.hibernate.tool.reveng.test.utils.FileUtil;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JavaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Josh Moore josh.moore@gmx.de
 * @author koen
 */
//TODO Reenable this test and make it pass (See HBX-2884)
@Disabled
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Properties.hbm.xml"
	};

	@TempDir
	public File outputFolder = new File("output");

	private DefaultArtifactCollector artifactCollector;
	private File outputDir = null;

	@BeforeEach
	public void setUp() throws Exception {
		artifactCollector = new DefaultArtifactCollector();
		outputDir = new File(outputFolder, "src");
		assertTrue(outputDir.mkdir());
		File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		MetadataDescriptor metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.getProperties().put(ExporterConstants.ARTIFACT_COLLECTOR, artifactCollector);
		exporter.start();
	}

	@Test
	public void testNoGenerationOfEmbeddedPropertiesComponent() {
		assertEquals(2, artifactCollector.getFileCount("java"));
	}

	@Test
	public void testGenerationOfEmbeddedProperties() {
		assertNotNull(
				FileUtil.findFirstString(
						"name",
						new File(outputDir, "properties/PPerson.java" )));
		assertNull(
				FileUtil.findFirstString(
						"emergencyContact",
						new File(outputDir, "properties/PPerson.java" )),
				"Embedded component/properties should not show up in .java");
	}

	@Test
	public void testCompilable() throws Exception {
		String propertiesUsageResourcePath = "/org/hibernate/tool/hbm2x/PropertiesTest/PropertiesUsage.java_";
		File propertiesUsageOrigin = new File(Objects.requireNonNull(getClass().getResource(propertiesUsageResourcePath)).toURI());
		File propertiesUsageDestination = new File(outputDir, "properties/PropertiesUsage.java");
		File targetDir = new File(outputFolder, "target" );
		assertTrue(targetDir.mkdir());
		Files.copy(propertiesUsageOrigin.toPath(), propertiesUsageDestination.toPath());
		JavaUtil.compile(outputDir, targetDir);
		assertTrue(new File(targetDir, "properties/PCompany.class").exists());
		assertTrue(new File(targetDir, "properties/PPerson.class").exists());
		assertTrue(new File(targetDir, "properties/PropertiesUsage.class").exists());
	}

}
