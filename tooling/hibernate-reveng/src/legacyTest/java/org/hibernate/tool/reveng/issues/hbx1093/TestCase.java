/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.issues.hbx1093;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.core.RevengSettings;
import org.hibernate.tool.reveng.internal.core.strategy.AbstractStrategy;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
