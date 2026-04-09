package org.hibernate.tool.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hibernate.tool.it.gradle.TestTemplate;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

public class TutorialTest extends TestTemplate {

	@BeforeEach
	public void beforeEach() {
		setGradleTaskToPerform("generateJava");
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))"
		});
	}
	
	@Test
	public void testTutorial() throws Exception {
		createProjectAndExecuteGradleCommand();
		File generatedOutputFolder = new File(getProjectDir(), "app/generated-sources");
		assertTrue(generatedOutputFolder.exists());
		assertTrue(generatedOutputFolder.isDirectory());
		assertEquals(1, generatedOutputFolder.list().length);
		File generatedPersonJavaFile = new File(generatedOutputFolder, "Person.java");
		assertTrue(generatedPersonJavaFile.exists());
		assertTrue(generatedPersonJavaFile.isFile());
	}
	
}
