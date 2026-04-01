/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbmlint.HbmLintTest;

import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.export.lint.*;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"CachingSettings.hbm.xml",
			"IdentifierIssues.hbm.xml",
			"BrokenLazy.hbm.xml"
	};

	@TempDir
	public File outputDir = new File("output");

	private MetadataDescriptor metadataDescriptor = null;

	@BeforeEach
	public void setUp() {
		File resourcesDir = new File(outputDir, "resources");
		assertTrue(resourcesDir.mkdir());
		metadataDescriptor = HibernateUtil.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
	}

	@Test
	public void testExporter() {
		HbmLintExporter exporter = new HbmLintExporter();
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();
	}

	@Test
	public void testValidateCache() {
		HbmLint analyzer = new HbmLint(new Detector[] { new BadCachingDetector() });
		analyzer.analyze(metadataDescriptor.createMetadata());
		assertEquals(1,analyzer.getResults().size());
	}

	@Test
	public void testValidateIdentifier() {
		HbmLint analyzer = new HbmLint(new Detector[] { new ShadowedIdentifierDetector() });
		analyzer.analyze(metadataDescriptor.createMetadata());
		assertEquals(1,analyzer.getResults().size());
	}

	@Test
	public void testBytecodeRestrictions() {
		HbmLint analyzer = new HbmLint(new Detector[] { new InstrumentationDetector() });
		analyzer.analyze(metadataDescriptor.createMetadata());
		assertEquals(2,analyzer.getResults().size(), analyzer.getResults().toString());
	}

}
