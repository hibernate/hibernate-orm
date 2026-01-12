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

import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.export.lint.*;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

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
