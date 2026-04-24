/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.issues.hbx2840;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

/**
 * Test verifies that a foreign key to a table with a composite ID works.
 */
public class TestCase {

	@TempDir
	public File outputDir = new File( "output" );

	@BeforeEach
	void setUp() {
		JdbcUtil.createDatabase( this );
		Exporter exporter = ExporterFactory.createExporter( ExporterType.JAVA );
		exporter.getProperties().put(
				ExporterConstants.METADATA_DESCRIPTOR,
				MetadataDescriptorFactory.createReverseEngineeringDescriptor( null, null )
		);
		exporter.getProperties().put( ExporterConstants.DESTINATION_FOLDER, outputDir );
		exporter.getProperties().put( ExporterConstants.TEMPLATE_PATH, new String[0] );
		exporter.getProperties().setProperty( "ejb3", "true" );
		exporter.start();
	}

	@AfterEach
	void tearDown() {
		JdbcUtil.dropDatabase( this );
	}

	@Test
	void testFileExistence() {
		JUnitUtil.assertIsNonEmptyFile( new File( outputDir.getAbsolutePath() + "/Parent.java" ) );
		JUnitUtil.assertIsNonEmptyFile( new File( outputDir.getAbsolutePath() + "/Child.java" ) );
		JUnitUtil.assertIsNonEmptyFile( new File( outputDir.getAbsolutePath() + "/ParentId.java" ) );
		JUnitUtil.assertIsNonEmptyFile( new File( outputDir.getAbsolutePath() + "/ChildId.java" ) );
	}
}
