/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import org.apache.maven.project.MavenProject;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

public class GenerateHbmMojoTest {

	private static final String CREATE_PERSON_TABLE =
			"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))";
	private static final String DROP_PERSON_TABLE =
			"drop table PERSON";

	@TempDir
	private File tempDir;

	private File outputDirectory;
	private GenerateHbmMojo generateHbmMojo;

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
	public void testGenerateHbm() throws Exception {
		generateHbmMojo.executeExporter(createMetadataDescriptor());
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
		generateHbmMojo = new GenerateHbmMojo();
		Field projectField = AbstractGenerationMojo.class.getDeclaredField("project");
		projectField.setAccessible(true);
		projectField.set(generateHbmMojo, new MavenProject());
		Field propertyFileField = AbstractGenerationMojo.class.getDeclaredField("propertyFile");
		propertyFileField.setAccessible(true);
		propertyFileField.set(generateHbmMojo, new File(tempDir, "hibernate.properties"));
		Field outputDirectoryField = GenerateHbmMojo.class.getDeclaredField("outputDirectory");
		outputDirectoryField.setAccessible(true);
		outputDirectoryField.set(generateHbmMojo, outputDirectory);
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
		return "jdbc:h2:" + tempDir.getAbsolutePath() + "/database/hbm_test;AUTO_SERVER=TRUE";
	}
}
