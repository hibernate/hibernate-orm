package org.hibernate.tool.gradle.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import org.hibernate.tool.it.gradle.TestTemplate;

public class NoAnnotationsTest extends TestTemplate {
	
	private static final List<String> GRADLE_INIT_PROJECT_ARGUMENTS = List.of(
			"init", "--type", "java-application", "--dsl", "groovy", "--test-framework", "junit-jupiter", "--java-version", "17");
	
	private File gradlePropertiesFile;
	private File gradleBuildFile;
	private File databaseFile;
	
	@Test
	public void testTutorial() throws Exception {
		assertTrue(getProjectDir().exists());
		createGradleProject();
		editGradleBuildFile();
		editGradlePropertiesFile();
		createDatabase();
		createHibernatePropertiesFile();
		verifyDatabase();
		executeGenerateJavaTask();
		verifyProject();
	}
	
	private void verifyDatabase() throws Exception {
		Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
		ResultSet resultSet = connection.createStatement().executeQuery("select * from PERSON");
		assertTrue(resultSet.next());
		assertEquals(1, resultSet.getInt(1));
		assertEquals("foo", resultSet.getString(2));
	}
	
	private void createGradleProject() throws Exception {
		GradleRunner runner = GradleRunner.create();
		runner.withArguments(GRADLE_INIT_PROJECT_ARGUMENTS);
		runner.forwardOutput();
		runner.withProjectDir(getProjectDir());
		BuildResult buildResult = runner.build();
		assertTrue(buildResult.getOutput().contains("BUILD SUCCESSFUL"));
		gradlePropertiesFile = new File(getProjectDir(), "gradle.properties");
		assertTrue(gradlePropertiesFile.exists());
		assertTrue(gradlePropertiesFile.isFile());
		File appDir = new File(getProjectDir(), "app");
		assertTrue(appDir.exists());
		assertTrue(appDir.isDirectory());
		gradleBuildFile = new File(appDir, "build.gradle");
		assertTrue(gradleBuildFile.exists());
		assertTrue(gradleBuildFile.isFile());
		databaseFile = new File(getProjectDir(), "database/test.mv.db");
		assertFalse(databaseFile.exists());
	}
	
	private void editGradleBuildFile() throws Exception {
		StringBuffer gradleBuildFileContents = new StringBuffer(
				new String(Files.readAllBytes(gradleBuildFile.toPath())));
		addHibernateToolsPluginLine(gradleBuildFileContents);
		addH2DatabaseDependencyLine(gradleBuildFileContents);
		addHibernateToolsExtension(gradleBuildFileContents);
		Files.writeString(gradleBuildFile.toPath(), gradleBuildFileContents.toString());
	}
	
	private void editGradlePropertiesFile() throws Exception {
		// The Hibernate Tools Gradle plugin does not support the configuration cache.
		// As this is enabled by default when initializing a new Gradle project, the setting needs to be commented out
		// in the gradle.properties file.
		StringBuffer gradlePropertiesFileContents = new StringBuffer(
				new String(Files.readAllBytes(gradlePropertiesFile.toPath())));
		int pos = gradlePropertiesFileContents.indexOf("org.gradle.configuration-cache=true");
		gradlePropertiesFileContents.insert(pos, "#");
		Files.writeString(gradlePropertiesFile.toPath(), gradlePropertiesFileContents.toString());
	}
	
	private void createDatabase() throws Exception {
		String CREATE_PERSON_TABLE = "create table PERSON (ID int not null, NAME varchar(20), primary key (ID))";
		Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
		Statement statement = connection.createStatement();
		statement.execute(CREATE_PERSON_TABLE);
		statement.execute("insert into PERSON values (1, 'foo')");
		statement.close();
		connection.close();	
		assertTrue(databaseFile.exists());
		assertTrue(databaseFile.isFile());
	}
	
	private void createHibernatePropertiesFile() throws Exception {
		File hibernatePropertiesFile = new File(getProjectDir(), "app/src/main/resources/hibernate.properties");
		StringBuffer hibernatePropertiesFileContents = new StringBuffer();	
		hibernatePropertiesFileContents
			.append("hibernate.connection.driver_class=org.h2.Driver\n")
			.append("hibernate.connection.url=" + constructJdbcConnectionString() + "\n")
			.append("hibernate.connection.username=\n")
			.append("hibernate.connection.password=\n")
			.append("hibernate.default_catalog=TEST\n")
			.append("hibernate.default_schema=PUBLIC\n");
		Files.writeString(hibernatePropertiesFile.toPath(), hibernatePropertiesFileContents.toString());
		assertTrue(hibernatePropertiesFile.exists());
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
		assertEquals(1, generatedOutputFolder.list().length);
		File generatedPersonJavaFile = new File(generatedOutputFolder, "Person.java");
		assertTrue(generatedPersonJavaFile.exists());
		assertTrue(generatedPersonJavaFile.isFile());
		String generatedPersonJavaFileContents = new String(
				Files.readAllBytes(generatedPersonJavaFile.toPath()));
		assertFalse(generatedPersonJavaFileContents.contains("import jakarta.persistence.Entity;"));
		assertTrue(generatedPersonJavaFileContents.contains("public class Person "));
	}
	
	private void addHibernateToolsPluginLine(StringBuffer gradleBuildFileContents) {
		int pos = gradleBuildFileContents.indexOf("plugins {");
		pos = gradleBuildFileContents.indexOf("}", pos);
		gradleBuildFileContents.insert(pos, constructHibernateToolsPluginLine() + "\n");		
	}
	
	private void addH2DatabaseDependencyLine(StringBuffer gradleBuildFileContents) {
		int pos = gradleBuildFileContents.indexOf("dependencies {");
		pos = gradleBuildFileContents.indexOf("}", pos);
		gradleBuildFileContents.insert(pos, constructH2DatabaseDependencyLine() + "\n");		
	}
	
	private void addHibernateToolsExtension(StringBuffer gradleBuildFileContents) {
		int pos = gradleBuildFileContents.indexOf("dependencies {");
		pos = gradleBuildFileContents.indexOf("}", pos);
		StringBuffer hibernateToolsExtension = new StringBuffer();
		hibernateToolsExtension
			.append("\n")
			.append("\n")
			.append("hibernateTools { \n")
			.append("  generateAnnotations=false \n")
			.append("}");
		gradleBuildFileContents.insert(pos + 1, hibernateToolsExtension.toString());
	}
	
	private String constructJdbcConnectionString() {
		return "jdbc:h2:" + getProjectDir().getAbsolutePath() + "/database/test;AUTO_SERVER=TRUE";
	}
	
	private String constructHibernateToolsPluginLine() {
		return "    id 'org.hibernate.tool.hibernate-tools-gradle' version '"
				+ System.getenv("HIBERNATE_TOOLS_VERSION") + "'";
	}
	
	private String constructH2DatabaseDependencyLine() {
		return "    implementation 'com.h2database:h2:" + System.getenv("H2_VERSION") + "'";
	}

}
