/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.HashcodeEqualsTest;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.export.common.DefaultArtifactCollector;
import org.hibernate.tool.reveng.test.utils.FileUtil;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JavaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"HashEquals.hbm.xml"
	};

	@TempDir
	public File outputFolder = new File("output");

	private File srcDir = null;
	private DefaultArtifactCollector artifactCollector = null;
	private MetadataDescriptor metadataDescriptor = null;

	@BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "output");
		assertTrue(srcDir.mkdir());
		File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		artifactCollector = new DefaultArtifactCollector();
		exporter.getProperties().put(ExporterConstants.ARTIFACT_COLLECTOR, artifactCollector);
		exporter.start();
	}

	@Test
	public void testJDK5FailureExpectedOnJDK4() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		exporter.getProperties().setProperty("jdk5", "true");
		artifactCollector = new DefaultArtifactCollector();
		exporter.getProperties().put(ExporterConstants.ARTIFACT_COLLECTOR, artifactCollector);
		exporter.start();
		testFileExistence();
		testNoVelocityLeftOvers();
		testCompilable();
	}

	@Test
	public void testFileExistence() {
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir, "org/hibernate/tool/hbm2x/HashEquals.java"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				srcDir, "org/hibernate/tool/hbm2x/Address.java"));
		assertEquals(2, artifactCollector.getFileCount("java"));
	}

	@Test
	public void testCompilable() {
		File compiled = new File(outputFolder, "compiled");
		assertTrue(compiled.mkdir());
		JavaUtil.compile(srcDir, compiled);
		JUnitUtil.assertIsNonEmptyFile(new File(
				compiled, "org/hibernate/tool/hbm2x/HashEquals.class"));
		JUnitUtil.assertIsNonEmptyFile(new File(
				compiled, "org/hibernate/tool/hbm2x/Address.class"));
	}

	@Test
	public void testNoVelocityLeftOvers() {
		assertNull( FileUtil
				.findFirstString(
						"$",
						new File(
								srcDir,
								"org/hibernate/tool/hbm2x/HashEquals.java")));
		assertNull(FileUtil
				.findFirstString(
						"$",
						new File(
								srcDir,
								"org/hibernate/tool/hbm2x/Address.java")));
	}

}
