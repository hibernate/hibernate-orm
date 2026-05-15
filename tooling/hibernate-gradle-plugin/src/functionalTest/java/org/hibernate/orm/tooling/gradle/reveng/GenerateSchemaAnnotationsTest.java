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
				"create table \"author\" (\"id\" int not null, primary key (\"id\"))",
				"create table \"book\" (\"isbn\" varchar(20) not null, \"code\" varchar(20) unique, "
						+ "\"pages\" int, \"author_id\" int, primary key (\"isbn\"), "
						+ "constraint \"fk_book_author\" foreign key (\"author_id\") references \"author\"(\"id\"))"
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
		assertTrue( generatedContents.contains( "@TableMapping(@Table(name = \"book\"))" ) );
		assertTrue( generatedContents.contains( "public @interface BOOK" ) );
		assertTrue( generatedContents.contains(
				"@ColumnMapping(@Column(name = \"isbn\", nullable = false, unique = false, length = 20, precision = 0, scale = 0))"
		) );
		assertTrue( generatedContents.contains( "@interface ISBN" ) );
		assertTrue( generatedContents.contains(
				"@ColumnMapping(@Column(name = \"code\", nullable = true, unique = true, length = 20, precision = 0, scale = 0))"
		) );
		assertTrue( generatedContents.contains( "@interface CODE" ) );
		assertFalse( generatedContents.contains( "@ColumnMapping(@Column(name = \"pages\"" ) );
		assertFalse( generatedContents.contains( "@interface PAGES" ) );
		assertTrue( generatedContents.contains(
				"@JoinColumnMapping(@JoinColumn(name = \"author_id\", referencedColumnName = \"id\", nullable = true))"
		) );
		assertFalse( generatedContents.contains( "@ColumnMapping(@Column(name = \"author_id\"" ) );
		assertTrue( generatedContents.contains( "@interface AUTHOR_ID" ) );
	}

	@Override
	protected void createProject() throws Exception {
		super.createProject();
		createRevengFile();
	}

	private void createRevengFile() throws Exception {
		String revengXml = "<hibernate-reverse-engineering>\n" +
				"  <table name=\"book\">\n" +
				"    <column name=\"pages\" exclude=\"true\"/>\n" +
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
