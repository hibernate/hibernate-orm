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

package org.hibernate.tool.hbm2x.HashcodeEqualsTest;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.export.common.DefaultArtifactCollector;
import org.hibernate.tool.test.utils.FileUtil;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.hibernate.tool.test.utils.JavaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

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
		assertNull(FileUtil
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
