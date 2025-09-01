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
	public void testNoGenerics() throws Exception {
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null,  NAME varchar(20), primary key (ID))",
				"create table ITEM (ID int not null,  NAME varchar(20), OWNER_ID int not null, " +
						"   primary key (ID), foreign key (OWNER_ID) references PERSON(ID))"
		});
		setHibernateToolsExtensionSection(
				"hibernateTools { \n" +
				"  useGenerics=false \n" +
				"}"
		);
		createProject();
		executeGradleCommand("generateJava");
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

}
