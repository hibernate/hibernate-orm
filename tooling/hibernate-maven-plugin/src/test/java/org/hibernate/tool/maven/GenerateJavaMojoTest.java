package org.hibernate.tool.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GenerateJavaMojoTest {

	private static final String JDBC_CONNECTION = "jdbc:h2:mem:test";
	private static final String CREATE_PERSON_TABLE =
			"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))";
	private static final String CREATE_ITEM_TABLE =
			"create table ITEM (ID int not null, NAME varchar(20), OWNER_ID int not null, primary key (ID), foreign key (OWNER_ID) references PERSON(ID))";
	private static final String DROP_ITEM_TABLE =
			"drop table ITEM";
	private static final String DROP_PERSON_TABLE =
			"drop table PERSON";

	@TempDir
	private File tempDir;
	
	private File outputDirectory;
	private GenerateJavaMojo generateJavaMojo;

	@BeforeEach
	public void beforeEach() throws Exception {
		createDatabase();
		createOutputDirectory();
		createGenerateJavaMojo();
	}
	
	@AfterEach
	public void afterEach() throws Exception {
		dropDatabase();
	}
	
	@Test
	public void testGenerateAnnotatedJava() throws Exception {
		File personJavaFile = new File(outputDirectory, "Person.java");
		// Person.java should not exist
		assertFalse(personJavaFile.exists());
		// Set value of field 'ejb3' to 'true' and execute mojo
		Field ejb3Field = GenerateJavaMojo.class.getDeclaredField("ejb3");
		ejb3Field.setAccessible(true);
		ejb3Field.set(generateJavaMojo, true);
		// Execute mojo
		generateJavaMojo.executeExporter(createMetadataDescriptor());
		// Person.java should exist
		assertTrue(personJavaFile.exists());
		// Person.java should be an annotated entity
		byte[] raw = Files.readAllBytes(personJavaFile.toPath());
		assertTrue(new String(raw).contains("import jakarta.persistence.Entity;"));
	}

	@Test
	public void testGenerateNonAnnotatedJava() throws Exception {		
		File personJavaFile = new File(outputDirectory, "Person.java");
		// Person.java should not exist
		assertFalse(personJavaFile.exists());
		// Set value of field 'ejb3' to 'false' and execute mojo
		Field ejb3Field = GenerateJavaMojo.class.getDeclaredField("ejb3");
		ejb3Field.setAccessible(true);
		ejb3Field.set(generateJavaMojo, false);
		// Execute mojo
		generateJavaMojo.executeExporter(createMetadataDescriptor());
		// Person.java should exist
		assertTrue(personJavaFile.exists());
		// Person.java should be an annotated entity
		byte[] raw = Files.readAllBytes(personJavaFile.toPath());
		assertFalse(new String(raw).contains("import jakarta.persistence.Entity;"));
	}

	@Test
	public void testGenerateJavaWithGenerics() throws Exception {
		File personJavaFile = new File(outputDirectory, "Person.java");
		// Person.java should not exist
		assertFalse(personJavaFile.exists());
		// Set value of field 'jdk5' to 'true' and execute mojo
		Field jdk5Field = GenerateJavaMojo.class.getDeclaredField("jdk5");
		jdk5Field.setAccessible(true);
		jdk5Field.set(generateJavaMojo, true);
		// Execute mojo
		generateJavaMojo.executeExporter(createMetadataDescriptor());
		// Person.java should exist
		assertTrue(personJavaFile.exists());
		// Person.java should be an annotated entity
		byte[] raw = Files.readAllBytes(personJavaFile.toPath());
		assertTrue(new String(raw).contains("Set<Item>"));
	}

	@Test
	public void testGenerateJavaWithoutGenerics() throws Exception {
		File personJavaFile = new File(outputDirectory, "Person.java");
		// Person.java should not exist
		assertFalse(personJavaFile.exists());
		// Set value of field 'jdk5' to 'true' and execute mojo
		Field jdk5Field = GenerateJavaMojo.class.getDeclaredField("jdk5");
		jdk5Field.setAccessible(true);
		jdk5Field.set(generateJavaMojo, false);
		// Execute mojo
		generateJavaMojo.executeExporter(createMetadataDescriptor());
		// Person.java should exist
		assertTrue(personJavaFile.exists());
		// Person.java should be an annotated entity
		byte[] raw = Files.readAllBytes(personJavaFile.toPath());
		assertFalse(new String(raw).contains("Set<Item>"));
	}

	private void createDatabase() throws Exception {
		Connection connection = DriverManager.getConnection(JDBC_CONNECTION);
		Statement statement = connection.createStatement();
		statement.execute(CREATE_PERSON_TABLE);
		statement.execute(CREATE_ITEM_TABLE);
	}

	private void dropDatabase() throws Exception {
		Connection connection = DriverManager.getConnection(JDBC_CONNECTION);
		Statement statement = connection.createStatement();
		statement.execute(DROP_ITEM_TABLE);
		statement.execute(DROP_PERSON_TABLE);
	}

	private void createGenerateJavaMojo() throws Exception {
		generateJavaMojo = new GenerateJavaMojo();
		Field projectField = AbstractGenerationMojo.class.getDeclaredField("project");
		projectField.setAccessible(true);
		projectField.set(generateJavaMojo, new MavenProject());
		Field propertyFileField = AbstractGenerationMojo.class.getDeclaredField("propertyFile");
		propertyFileField.setAccessible(true);
		propertyFileField.set(generateJavaMojo, new File(tempDir, "hibernate.properties"));
		Field outputDirectoryField = GenerateJavaMojo.class.getDeclaredField("outputDirectory");
		outputDirectoryField.setAccessible(true);
		outputDirectoryField.set(generateJavaMojo, outputDirectory);
	}
	
	private void createOutputDirectory() {
		outputDirectory = new File(tempDir, "generated");
		outputDirectory.mkdir();
	}

	private MetadataDescriptor createMetadataDescriptor() {
		return MetadataDescriptorFactory.createReverseEngineeringDescriptor(
				null,
				createProperties());
	}

	private Properties createProperties() {
		Properties result = new Properties();
		result.put("hibernate.connection.url", JDBC_CONNECTION);
		result.put("hibernate.default_catalog", "TEST");
		result.put("hibernate.default_schema", "PUBLIC");
		return result;
	}

}
