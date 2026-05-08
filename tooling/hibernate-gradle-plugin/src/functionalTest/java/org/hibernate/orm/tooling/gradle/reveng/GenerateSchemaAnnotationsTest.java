/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import static java.lang.System.lineSeparator;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerateSchemaAnnotationsTest extends TestTemplate {

	@BeforeEach
	public void beforeEach() {
		setGradleTaskToPerform( "generateSchemaAnnotations" );
		setDatabaseCreationScript( new String[] {
				"create table AUTHOR (ID int not null, primary key (ID))",
				"create table BOOK (ISBN varchar(20) not null, CODE varchar(20) unique, PAGES int, AUTHOR_ID int, primary key (ISBN), "
						+ "constraint FK_BOOK_AUTHOR foreign key (AUTHOR_ID) references AUTHOR(ID))"
		} );
	}

	@Test
	void testGenerateSchemaAnnotations() throws Exception {
		createProjectAndExecuteGradleCommand();

		File generatedFile = new File(
				getProjectDir(),
				"app/build/generated/sources/schemaAnnotations/foo/schema/BOOK.java"
		);
		assertTrue( generatedFile.exists() );
		assertTrue( generatedFile.isFile() );

		String generatedContents = Files.readString( generatedFile.toPath() );
		assertTrue( generatedContents.contains( "package foo.schema;" ) );
		assertTrue( generatedContents.contains( "import org.hibernate.annotations.schema.JoinColumnMapping;" ) );
		assertTrue( generatedContents.contains( "import jakarta.persistence.JoinColumn;" ) );
		assertTrue( generatedContents.contains( "@TableMapping(@Table(name = \"BOOK\"))" ) );
		assertTrue( generatedContents.contains( "public @interface BOOK" ) );
		assertTrue( generatedContents.contains(
				"@ColumnMapping(@Column(name = \"ISBN\", nullable = false, unique = false, length = 20, precision = 0, scale = 0))"
		) );
		assertTrue( generatedContents.contains( "@interface ISBN" ) );
		assertTrue( generatedContents.contains(
				"@ColumnMapping(@Column(name = \"CODE\", nullable = true, unique = true, length = 20, precision = 0, scale = 0))"
		) );
		assertTrue( generatedContents.contains( "@interface CODE" ) );
		assertFalse( generatedContents.contains( "@ColumnMapping(@Column(name = \"PAGES\"" ) );
		assertFalse( generatedContents.contains( "@interface PAGES" ) );
		assertTrue( generatedContents.contains(
				"@JoinColumnMapping(@JoinColumn(name = \"AUTHOR_ID\", referencedColumnName = \"ID\", nullable = true))"
		) );
		assertFalse( generatedContents.contains( "@ColumnMapping(@Column(name = \"AUTHOR_ID\"" ) );
		assertTrue( generatedContents.contains( "@interface AUTHOR_ID" ) );
	}

	@Override
	protected void createProject() throws Exception {
		super.createProject();
		createRevengFile();
	}

	private void createRevengFile() throws Exception {
		String revengXml = "<hibernate-reverse-engineering>\n" +
				"  <table name=\"BOOK\">\n" +
				"    <column name=\"PAGES\" exclude=\"true\"/>\n" +
				"  </table>\n" +
				"</hibernate-reverse-engineering>";
		File resourcesFolder = new File( getProjectDir(), "app/src/main/resources" );
		resourcesFolder.mkdirs();
		Files.writeString( new File( resourcesFolder, "schema-annotations.reveng.xml" ).toPath(), revengXml );
	}

	@Override
	protected void addRevengExtension(StringBuffer gradleBuildFileContents) {
		StringBuilder configuration = new StringBuilder();
		configuration.append( "hibernate {" ).append( lineSeparator() );
		configuration.append( "  useSameVersion = false" ).append( lineSeparator() );
		configuration.append( "}" ).append( lineSeparator() );
		configuration.append( lineSeparator() );
		configuration.append( "tasks.named('generateSchemaAnnotations') {" ).append( lineSeparator() );
		configuration.append( "  packageName = 'foo.schema'" ).append( lineSeparator() );
		configuration.append( "  revengFile = 'schema-annotations.reveng.xml'" ).append( lineSeparator() );
		configuration.append( "}" );

		int pos = gradleBuildFileContents.indexOf( "dependencies {" );
		pos = gradleBuildFileContents.indexOf( "}", pos );
		gradleBuildFileContents.insert( pos + 1, lineSeparator() + lineSeparator() + configuration );
	}
}
