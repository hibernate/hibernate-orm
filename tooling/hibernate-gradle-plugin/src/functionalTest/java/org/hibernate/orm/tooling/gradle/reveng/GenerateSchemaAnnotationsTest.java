/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import static java.lang.System.lineSeparator;
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
				"create table BOOK (ISBN varchar(20) not null, PAGES int, primary key (ISBN))"
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
		assertTrue( generatedContents.contains( "@StaticTable(name = \"BOOK\")" ) );
		assertTrue( generatedContents.contains( "public @interface BOOK" ) );
		assertTrue( generatedContents.contains( "@StaticColumn(name = \"ISBN\", type = JDBCType.VARCHAR)" ) );
		assertTrue( generatedContents.contains( "public @interface ISBN" ) );
		assertTrue( generatedContents.contains( "@StaticColumn(name = \"PAGES\", type = JDBCType.INTEGER)" ) );
		assertTrue( generatedContents.contains( "public @interface PAGES" ) );
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
