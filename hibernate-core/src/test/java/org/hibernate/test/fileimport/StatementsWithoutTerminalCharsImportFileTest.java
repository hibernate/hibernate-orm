/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.fileimport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;
import org.hibernate.tool.schema.ast.SqlScriptParserException;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13673")
@RequiresDialect(value = H2Dialect.class,
		jiraKey = "HHH-6286",
		comment = "Only running the tests against H2, because the sql statements in the import file are not generic. " +
				"This test should actually not test directly against the db")
public class StatementsWithoutTerminalCharsImportFileTest extends BaseUnitTestCase {

	private static final String IMPORT_FILE = "org/hibernate/test/fileimport/statements-without-terminal-chars.sql";

	private static final String EXPECTED_ERROR_MESSAGE = "Import script Sql statements must terminate with a ';' char";

	@Test
	public void testImportFile() throws IOException {
		final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		final MultipleLinesSqlCommandExtractor extractor = new MultipleLinesSqlCommandExtractor();

		try ( final InputStream stream = classLoader.getResourceAsStream( IMPORT_FILE ) ) {
			assertThat( stream, notNullValue() );
			try (final InputStreamReader reader = new InputStreamReader( stream )) {
				extractor.extractCommands( reader );
			}

			fail( "ImportScriptException expected" );
		}
		catch (SqlScriptParserException e) {
			assertThat( e.getMessage(), endsWith( EXPECTED_ERROR_MESSAGE ) );
		}
	}
}
