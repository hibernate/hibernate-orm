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
package org.hibernate.tool.issues.hbx1093;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.internal.reveng.strategy.AbstractStrategy;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author koen
 */
public class TestCase {
	
	@TempDir
	public File outputDir = new File("output");
	
	private MetadataDescriptor metadataDescriptor = null;
	
	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
        AbstractStrategy c = new DefaultStrategy();
        c.setSettings(new RevengSettings(c).setDetectManyToMany(true)); 
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(c, null);
	}
	
	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testGenerateJava() throws IOException {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);	
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.getProperties().setProperty("ejb3", "true");
		exporter.start();
		File etManyToManyComp1 = new File(outputDir, "EtManyToManyComp1.java");
		String str = new String(Files.readAllBytes(etManyToManyComp1.toPath()));
		assertTrue(str.contains("@JoinColumn(name=\"FK_ET_MANY_TO_MANY_COMP22_ID\""));
		File etManyToManyComp2 = new File(outputDir, "EtManyToManyComp2.java");
		str = new String(Files.readAllBytes(etManyToManyComp2.toPath()));
		assertTrue(str.contains("@JoinColumn(name=\"FK_ET_MANY_TO_MANY_COMP11_ID\""));
	}
	
}
