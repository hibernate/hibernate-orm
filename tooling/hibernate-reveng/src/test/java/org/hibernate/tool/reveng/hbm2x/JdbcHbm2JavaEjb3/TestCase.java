/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.JdbcHbm2JavaEjb3;

import jakarta.persistence.Persistence;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.test.utils.FileUtil;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JavaUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author max
 * @author koen
 *
 */
public class TestCase {

	@TempDir
	public File outputDir = new File("output");

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(
				ExporterConstants.METADATA_DESCRIPTOR,
				MetadataDescriptorFactory.createReverseEngineeringDescriptor(null, null));
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		exporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[0]);
		exporter.getProperties().setProperty("ejb3", "true");
		exporter.getProperties().setProperty("jdk5", "true");
		exporter.start();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testFileExistence() {
		JUnitUtil.assertIsNonEmptyFile( new File( outputDir.getAbsolutePath() + "/Master.java") );
	}

	@Test
	public void testUniqueConstraints() {
		assertNull( FileUtil.findFirstString("uniqueConstraints", new File(outputDir, "Master.java")));
		assertNotNull(FileUtil.findFirstString( "uniqueConstraints", new File(outputDir,"Uniquemaster.java") ));
	}

	@Test
	public void testCompile() {
		File destination = new File(outputDir, "destination");
		assertTrue(destination.mkdir());
		List<String> jars = new ArrayList<>();
		jars.add( JavaUtil.resolvePathToJarFileFor(Persistence.class)); // for jpa api
		JavaUtil.compile(outputDir, destination, jars);
		JUnitUtil.assertIsNonEmptyFile(new File(destination, "Master.class"));
		JUnitUtil.assertIsNonEmptyFile(new File(destination, "Uniquemaster.class"));
	}

}
