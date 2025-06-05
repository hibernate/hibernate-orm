package org.hibernate.tool.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.sql.DriverManager;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GenerateJavaMojoTest {

	private static final String JDBC_CONNECTION = "jdbc:h2:mem:test";
	private static final String CREATE_PERSON_TABLE =
			"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))";
	private static final String DROP_PERSON_TABLE =
			"drop table PERSON";

	@TempDir
	private File tempDir;
	
	private File outputDirectory;
	private GenerateJavaMojo generateJavaMojo;

	@BeforeEach
	public void beforeEach() throws Exception {
		createDatabase();
		createPropertiesFile();
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
		// Execute mojo with default value of 'ejb3' field which is 'true'
		generateJavaMojo.execute();
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
		generateJavaMojo.execute();
		// Person.java should exist
		assertTrue(personJavaFile.exists());
		// Person.java should be an annotated entity
		byte[] raw = Files.readAllBytes(personJavaFile.toPath());
		assertFalse(new String(raw).contains("import jakarta.persistence.Entity;"));
	}

	private void createDatabase() throws Exception {
		DriverManager
			.getConnection(JDBC_CONNECTION)
			.createStatement()
			.execute(CREATE_PERSON_TABLE);
	}

	private void dropDatabase() throws Exception {
		DriverManager
			.getConnection(JDBC_CONNECTION)
			.createStatement()
			.execute(DROP_PERSON_TABLE);
	}

	private void createPropertiesFile() throws Exception {
		File propertiesFile = new File(tempDir, "hibernate.properties");
        try (FileWriter fileWriter = new FileWriter(propertiesFile)) {
			fileWriter.write("hibernate.connection.url=" + JDBC_CONNECTION + '\n');
			fileWriter.write("hibernate.default_catalog=TEST\n");
			fileWriter.write("hibernate.default_schema=PUBLIC\n");
        }
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


}
