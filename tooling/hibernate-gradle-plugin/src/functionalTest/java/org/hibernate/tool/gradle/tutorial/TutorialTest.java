package org.hibernate.tool.gradle.tutorial;

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

public class TutorialTest extends TestTemplate {
	
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
		String CREATE_PERSON_TABLE = "create table PERSON (ID int not null, NAME varchar(20), primary key (ID))";
		Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
		Statement statement = connection.createStatement();
		statement.execute(CREATE_PERSON_TABLE);
		statement.execute("insert into PERSON values (1, 'foo')");
		statement.close();
		connection.close();	
		assertTrue(getDatabaseFile().exists());
		assertTrue(getDatabaseFile().isFile());
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
	
	private void verifyProject() {
		File generatedOutputFolder = new File(getProjectDir(), "app/generated-sources");
		assertTrue(generatedOutputFolder.exists());
		assertTrue(generatedOutputFolder.isDirectory());
		assertEquals(1, generatedOutputFolder.list().length);
		File generatedPersonJavaFile = new File(generatedOutputFolder, "Person.java");
		assertTrue(generatedPersonJavaFile.exists());
		assertTrue(generatedPersonJavaFile.isFile());
	}
	
}
