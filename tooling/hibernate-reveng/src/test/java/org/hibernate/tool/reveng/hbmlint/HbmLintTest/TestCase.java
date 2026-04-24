/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbmlint.HbmLintTest;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.util.MetadataHelper;
import org.hibernate.tool.reveng.internal.exporter.lint.BadCachingDetector;
import org.hibernate.tool.reveng.internal.exporter.lint.HbmLintExporter;
import org.hibernate.tool.reveng.internal.exporter.lint.Issue;
import org.hibernate.tool.reveng.internal.exporter.lint.LintDetector;
import org.hibernate.tool.reveng.internal.exporter.lint.ShadowedIdentifierDetector;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
		Exporter exporter = ExporterFactory.createExporter(ExporterType.HBM_LINT);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.start();
	}

	@Test
	public void testValidateCache() {
		var entities = MetadataHelper.from(metadataDescriptor).getEntityClassDetails();
		HbmLintExporter linter = HbmLintExporter.create(
				entities, new LintDetector[] { new BadCachingDetector() });
		List<Issue> results = linter.analyze();
		assertEquals(1, results.size());
	}

	@Test
	public void testValidateIdentifier() {
		var entities = MetadataHelper.from(metadataDescriptor).getEntityClassDetails();
		HbmLintExporter linter = HbmLintExporter.create(
				entities, new LintDetector[] { new ShadowedIdentifierDetector() });
		List<Issue> results = linter.analyze();
		assertEquals(1, results.size());
	}

}
