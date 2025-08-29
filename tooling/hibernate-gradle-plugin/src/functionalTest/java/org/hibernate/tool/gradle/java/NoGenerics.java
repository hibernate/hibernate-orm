package org.hibernate.tool.gradle.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import org.hibernate.tool.it.gradle.TestTemplate;

public class NoGenerics extends TestTemplate {
	
	@Test
	public void testTutorial() throws Exception {
		assertTrue(getProjectDir().exists());
		createGradleProject();
		editGradleBuildFile();
		editGradlePropertiesFile();
		createDatabase();
		createHibernatePropertiesFile();
		executeGenerateJavaTask();
		verifyProject();
	}
	
	private void editGradlePropertiesFile() throws Exception {
		// The Hibernate Tools Gradle plugin does not support the configuration cache.
		// As this is enabled by default when initializing a new Gradle project, the setting needs to be commented out
		// in the gradle.properties file.
		StringBuffer gradlePropertiesFileContents = new StringBuffer(
				new String(Files.readAllBytes(getGradlePropertiesFile().toPath())));
		int pos = gradlePropertiesFileContents.indexOf("org.gradle.configuration-cache=true");
		gradlePropertiesFileContents.insert(pos, "#");
		Files.writeString(getGradlePropertiesFile().toPath(), gradlePropertiesFileContents.toString());
	}
	
	private void createDatabase() throws Exception {
		String CREATE_PERSON_TABLE =
			    "create table PERSON (ID int not null,  NAME varchar(20), primary key (ID))";
		String CREATE_ITEM_TABLE =
			    "create table ITEM (ID int not null,  NAME varchar(20), OWNER_ID int not null, " +
			    "   primary key (ID), foreign key (OWNER_ID) references PERSON(ID))";
		Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
		Statement statement = connection.createStatement();
		statement.execute(CREATE_PERSON_TABLE);
		statement.execute(CREATE_ITEM_TABLE);
		statement.close();
		connection.close();
		assertTrue(getDatabaseFile().exists());
		assertTrue(getDatabaseFile().isFile());
	}
	
	private void executeGenerateJavaTask() throws Exception {
		GradleRunner gradleRunner = GradleRunner.create();
		gradleRunner.forwardOutput();
		gradleRunner.withProjectDir(getProjectDir());
		gradleRunner.withPluginClasspath();
		gradleRunner.withArguments("generateJava");
		BuildResult buildResult = gradleRunner.build();
		assertTrue(buildResult.getOutput().contains("BUILD SUCCESSFUL"));
	}
	
	private void verifyProject() throws Exception {
		File generatedOutputFolder = new File(getProjectDir(), "app/generated-sources");
		assertTrue(generatedOutputFolder.exists());
		assertTrue(generatedOutputFolder.isDirectory());
		assertEquals(2, generatedOutputFolder.list().length);
		File generatedPersonJavaFile = new File(generatedOutputFolder, "Person.java");
		assertTrue(generatedPersonJavaFile.exists());
		assertTrue(generatedPersonJavaFile.isFile());
		String generatedPersonJavaFileContents = new String(
				Files.readAllBytes(generatedPersonJavaFile.toPath()));
		assertTrue(generatedPersonJavaFileContents.contains("public class Person "));
		assertFalse(generatedPersonJavaFileContents.contains("Set<Item>"));
		File generatedItemJavaFile = new File(generatedOutputFolder, "Item.java");
		assertTrue(generatedItemJavaFile.exists());
		assertTrue(generatedItemJavaFile.isFile());
		String generatedItemJavaFileContents = new String(
				Files.readAllBytes(generatedItemJavaFile.toPath()));
		assertTrue(generatedItemJavaFileContents.contains("public class Item "));
	}
	
	protected void addHibernateToolsExtension(StringBuffer gradleBuildFileContents) {
		int pos = gradleBuildFileContents.indexOf("dependencies {");
		pos = gradleBuildFileContents.indexOf("}", pos);
		StringBuffer hibernateToolsExtension = new StringBuffer();
		hibernateToolsExtension
			.append("\n")
			.append("\n")
			.append("hibernateTools { \n")
			.append("  useGenerics=false \n")
			.append("}");
		gradleBuildFileContents.insert(pos + 1, hibernateToolsExtension.toString());
	}
	
}
