/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.hibernate.tool.it.gradle.TestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerateJavaTest extends TestTemplate {

	@BeforeEach
	public void beforeEach() {
		setGradleCommandToExecute("generateJava");
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))"
		});
	}

	@Test
	public void testJpaDefault() throws Exception {
		createProjectAndExecuteGradleCommand();
		File generatedOutputFolder = new File(getProjectDir(), "app/generated-sources");
		assertTrue(generatedOutputFolder.exists());
		assertTrue(generatedOutputFolder.isDirectory());
		assertEquals(1, generatedOutputFolder.list().length);
		File generatedPersonJavaFile = new File(generatedOutputFolder, "Person.java");
		assertTrue(generatedPersonJavaFile.exists());
		assertTrue(generatedPersonJavaFile.isFile());
		String generatedPersonJavaFileContents = new String(
				Files.readAllBytes(generatedPersonJavaFile.toPath()));
		assertTrue(generatedPersonJavaFileContents.contains("import jakarta.persistence.Entity;"));
		assertTrue(generatedPersonJavaFileContents.contains("public class Person "));
	}

	@Test
	public void testNoAnnotations() throws Exception {
		setHibernateToolsExtensionSection(
				"hibernateTools { \n" +
						"  generateAnnotations=false \n" +
						"}"
		);
		createProjectAndExecuteGradleCommand();
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
		createProjectAndExecuteGradleCommand();
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

	@Test
	public void testUseGenerics() throws Exception {
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null,  NAME varchar(20), primary key (ID))",
				"create table ITEM (ID int not null,  NAME varchar(20), OWNER_ID int not null, " +
						"   primary key (ID), foreign key (OWNER_ID) references PERSON(ID))"
		});
		createProjectAndExecuteGradleCommand();
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
		assertTrue(generatedPersonJavaFileContents.contains("Set<Item>"));
		File generatedItemJavaFile = new File(generatedOutputFolder, "Item.java");
		assertTrue(generatedItemJavaFile.exists());
		assertTrue(generatedItemJavaFile.isFile());
		String generatedItemJavaFileContents = new String(
				Files.readAllBytes(generatedItemJavaFile.toPath()));
		assertTrue(generatedItemJavaFileContents.contains("public class Item "));
	}

	@Test
	public void testPackageName() throws Exception {
		setHibernateToolsExtensionSection(
				"hibernateTools { \n" +
				"  packageName = 'foo.model' \n" +
				"}"
		);
		createProjectAndExecuteGradleCommand();
		File generatedSourcesFolder = new File(getProjectDir(), "app/generated-sources");
		assertTrue(generatedSourcesFolder.exists());
		assertTrue(generatedSourcesFolder.isDirectory());
		File fooFile = new File(generatedSourcesFolder, "foo/model/Person.java");
		assertTrue(fooFile.exists());
		assertTrue(fooFile.isFile());
	}

}
