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
				"create table BOOK (ISBN varchar(20) not null, PAGES int, AUTHOR_ID int, primary key (ISBN), "
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
		assertTrue( generatedContents.contains( "import org.hibernate.annotations.schema.StaticJoinColumn;" ) );
		assertTrue( generatedContents.contains( "@StaticTable(name = \"BOOK\")" ) );
		assertTrue( generatedContents.contains( "public @interface BOOK" ) );
		assertTrue( generatedContents.contains(
				"@StaticColumn(name = \"ISBN\", type = JDBCType.VARCHAR, nullable = false, length = 20, precision = 0, scale = 0)"
		) );
		assertTrue( generatedContents.contains( "public @interface ISBN" ) );
		assertTrue( generatedContents.matches(
				"(?s).*@StaticColumn\\(name = \"PAGES\", type = JDBCType\\.INTEGER, nullable = true, length = 255, precision = \\d+, scale = 0\\).*"
		) );
		assertTrue( generatedContents.contains( "public @interface PAGES" ) );
		assertTrue( generatedContents.contains(
				"@StaticJoinColumn(name = \"AUTHOR_ID\", referencedTableName = \"AUTHOR\", referencedColumnName = \"ID\", type = JDBCType.INTEGER, nullable = true)"
		) );
		assertFalse( generatedContents.contains( "@StaticColumn(name = \"AUTHOR_ID\"" ) );
		assertTrue( generatedContents.contains( "public @interface AUTHOR_ID" ) );
	}

	@Override
	protected void addRevengExtension(StringBuffer gradleBuildFileContents) {
		StringBuilder configuration = new StringBuilder();
		configuration.append( "hibernate {" ).append( lineSeparator() );
		configuration.append( "  useSameVersion = false" ).append( lineSeparator() );
		configuration.append( "}" ).append( lineSeparator() );
		configuration.append( lineSeparator() );
		configuration.append( "tasks.named('generateSchemaAnnotations') {" ).append( lineSeparator() );
		configuration.append( "  jdbcDriver = 'org.h2.Driver'" ).append( lineSeparator() );
		configuration.append( "  jdbcUrl = '" ).append( constructJdbcConnectionString() ).append( "'" )
				.append( lineSeparator() );
		configuration.append( "  schemaName = 'PUBLIC'" ).append( lineSeparator() );
		configuration.append( "  packageName = 'foo.schema'" ).append( lineSeparator() );
		configuration.append( "}" );

		int pos = gradleBuildFileContents.indexOf( "dependencies {" );
		pos = gradleBuildFileContents.indexOf( "}", pos );
		gradleBuildFileContents.insert( pos + 1, lineSeparator() + lineSeparator() + configuration );
	}
}
