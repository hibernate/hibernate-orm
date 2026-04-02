/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import org.apache.maven.project.MavenProject;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenerateDdlMojoTest {

	private static final String CREATE_PERSON_TABLE =
			"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))";
	private static final String DROP_PERSON_TABLE =
			"drop table PERSON";

	@TempDir
	private File tempDir;

	private File outputDirectory;
	private GenerateDdlMojo generateDdlMojo;

	@BeforeEach
	public void beforeEach() throws Exception {
		createDatabase();
		createOutputDirectory();
		createMojo();
	}

	@AfterEach
	public void afterEach() throws Exception {
		dropDatabase();
	}

	@Test
	public void testGenerateDdl() throws Exception {
		generateDdlMojo.executeExporter(createMetadataDescriptor());
		File schemaFile = new File(outputDirectory, "schema.ddl");
		assertTrue(schemaFile.exists(), "Expected schema.ddl to be generated");
	}

	private void createDatabase() throws Exception {
		Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
		Statement statement = connection.createStatement();
		statement.execute(CREATE_PERSON_TABLE);
		statement.close();
		connection.close();
	}

	private void dropDatabase() throws Exception {
		Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
		Statement statement = connection.createStatement();
		statement.execute(DROP_PERSON_TABLE);
		statement.close();
		connection.close();
	}

	private void createMojo() throws Exception {
		generateDdlMojo = new GenerateDdlMojo();
		Field projectField = AbstractGenerationMojo.class.getDeclaredField("project");
		projectField.setAccessible(true);
		projectField.set(generateDdlMojo, new MavenProject());
		Field propertyFileField = AbstractGenerationMojo.class.getDeclaredField("propertyFile");
		propertyFileField.setAccessible(true);
		propertyFileField.set(generateDdlMojo, new File(tempDir, "hibernate.properties"));
		// Set outputDirectory
		Field outputDirectoryField = GenerateDdlMojo.class.getDeclaredField("outputDirectory");
		outputDirectoryField.setAccessible(true);
		outputDirectoryField.set(generateDdlMojo, outputDirectory);
		// Set outputFileName
		Field outputFileNameField = GenerateDdlMojo.class.getDeclaredField("outputFileName");
		outputFileNameField.setAccessible(true);
		outputFileNameField.set(generateDdlMojo, "schema.ddl");
		// Set targetTypes
		Field targetTypesField = GenerateDdlMojo.class.getDeclaredField("targetTypes");
		targetTypesField.setAccessible(true);
		targetTypesField.set(generateDdlMojo, EnumSet.of(TargetType.SCRIPT));
		// Set schemaExportAction
		Field schemaExportActionField = GenerateDdlMojo.class.getDeclaredField("schemaExportAction");
		schemaExportActionField.setAccessible(true);
		schemaExportActionField.set(generateDdlMojo, SchemaExport.Action.CREATE);
		// Set delimiter
		Field delimiterField = GenerateDdlMojo.class.getDeclaredField("delimiter");
		delimiterField.setAccessible(true);
		delimiterField.set(generateDdlMojo, ";");
		// Set format
		Field formatField = GenerateDdlMojo.class.getDeclaredField("format");
		formatField.setAccessible(true);
		formatField.set(generateDdlMojo, true);
		// Set haltOnError
		Field haltOnErrorField = GenerateDdlMojo.class.getDeclaredField("haltOnError");
		haltOnErrorField.setAccessible(true);
		haltOnErrorField.set(generateDdlMojo, true);
	}

	private void createOutputDirectory() {
		outputDirectory = new File(tempDir, "generated");
		if (!outputDirectory.mkdir()) throw new RuntimeException("Unable to create output directory: " + outputDirectory);
	}

	private MetadataDescriptor createMetadataDescriptor() {
		return MetadataDescriptorFactory.createReverseEngineeringDescriptor(null, createProperties());
	}

	private Properties createProperties() {
		Properties result = new Properties();
		result.put("hibernate.connection.url", constructJdbcConnectionString());
		result.put("hibernate.default_catalog", "TEST");
		result.put("hibernate.default_schema", "PUBLIC");
		return result;
	}

	private String constructJdbcConnectionString() {
		return "jdbc:h2:" + tempDir.getAbsolutePath() + "/database/ddl_test;AUTO_SERVER=TRUE";
	}
}
