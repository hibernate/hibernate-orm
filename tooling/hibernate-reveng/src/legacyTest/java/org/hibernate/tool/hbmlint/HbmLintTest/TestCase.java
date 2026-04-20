/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.hbmlint.HbmLintTest;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.util.MetadataHelper;
import org.hibernate.tool.internal.exporter.lint.BadCachingDetector;
import org.hibernate.tool.internal.exporter.lint.HbmLintExporter;
import org.hibernate.tool.internal.exporter.lint.Issue;
import org.hibernate.tool.internal.exporter.lint.LintDetector;
import org.hibernate.tool.internal.exporter.lint.ShadowedIdentifierDetector;
import org.hibernate.tool.test.utils.HibernateUtil;
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
