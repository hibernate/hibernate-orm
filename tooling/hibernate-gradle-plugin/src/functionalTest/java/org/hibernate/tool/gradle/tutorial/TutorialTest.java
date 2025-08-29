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
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))",
				"insert into PERSON values (1, 'foo')"
		});
		assertTrue(getProjectDir().exists());
		createGradleProject();
		editGradleBuildFile();
		editGradlePropertiesFile();
		createDatabase();
		createHibernatePropertiesFile();
		executeGenerateJavaTask();
		verifyProject();
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
