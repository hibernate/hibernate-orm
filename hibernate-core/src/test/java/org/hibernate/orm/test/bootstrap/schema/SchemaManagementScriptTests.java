/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.schema;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for parsing auto-schema-management "import" scripts whether from
 *
 * 		* Hibernate `import.sql` script
 * 		* JPA's create and drop script source handling
 *
 * @author Steve Ebersole
 */
public class SchemaManagementScriptTests {
	private final Dialect dialect = new H2Dialect();

	private final String FIRST = "first  statement";
	private final String SECOND = "second  statement";
	private final String THIRD = "third  statement";

	@Test
	public void testSimpleExtraction() {
		final String firstCommand = FIRST;
		final String secondCommand = SECOND;
		final String thirdCommand = THIRD;

		final String commands = firstCommand + "; " + secondCommand + "; " + thirdCommand + ";";
		final Reader reader = new BufferedReader( new StringReader( commands ) );

		final MultiLineSqlScriptExtractor extractor = new MultiLineSqlScriptExtractor();
		final String[] extractedCommands = extractor.extractCommands( reader, dialect ).toArray( new String[0] );

		assertThat( extractedCommands.length, is( 3 ) );
		assertThat( extractedCommands[0], is( FIRST ) );
		assertThat( extractedCommands[1], is( SECOND ) );
		assertThat( extractedCommands[2], is( THIRD ) );
	}

	@Test
	public void testMultiLineExtraction() {
		final String firstCommand = "first " + endOfLine() + "statement";
		final String secondCommand = "second " + endOfLine() + "statement";
		final String thirdCommand = "third " + endOfLine() + "statement";

		final String commands = firstCommand + "; " + secondCommand + "; " + thirdCommand + ";";
		final Reader reader = new BufferedReader( new StringReader( commands ) );

		final MultiLineSqlScriptExtractor extractor = new MultiLineSqlScriptExtractor();
		final String[] extractedCommands = extractor.extractCommands( reader, dialect ).toArray( new String[0] );

		assertThat( extractedCommands.length, is( 3 ) );
		assertThat( extractedCommands[0], is( FIRST ) );
		assertThat( extractedCommands[1], is( SECOND ) );
		assertThat( extractedCommands[2], is( THIRD ) );
	}

	private String endOfLine() {
		return System.lineSeparator();
	}

	@Test
	public void testMultiLineCommentExtraction() {
		final String firstCommand = "first " + endOfLine() + "statement";
		final String secondCommand = "second " + endOfLine() + "statement";
		final String thirdCommand = "third " + endOfLine() + "/*" + endOfLine() + ";" + endOfLine() + "*/statement";

		final String commands = firstCommand + "; " + secondCommand + "; " + thirdCommand + ";";
		final Reader reader = new BufferedReader( new StringReader( commands ) );

		final MultiLineSqlScriptExtractor extractor = new MultiLineSqlScriptExtractor();
		final String[] extractedCommands = extractor.extractCommands( reader, dialect ).toArray( new String[0] );

		assertThat( extractedCommands.length, is( 3 ) );
		assertThat( extractedCommands[0], is( FIRST ) );
		assertThat( extractedCommands[1], is( SECOND ) );
		assertThat( extractedCommands[2], is( THIRD ) );
	}
}
