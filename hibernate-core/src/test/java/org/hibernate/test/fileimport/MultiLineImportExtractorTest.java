/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fileimport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class MultiLineImportExtractorTest {
	public static final String IMPORT_FILE = "org/hibernate/test/fileimport/multi-line-statements.sql";

	private final MultipleLinesSqlCommandExtractor extractor = new MultipleLinesSqlCommandExtractor();

	@Test
	public void testExtraction() throws IOException {
		final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

		try (final InputStream stream = classLoader.getResourceAsStream( IMPORT_FILE )) {
			assertThat( stream, notNullValue() );
			try (final InputStreamReader reader = new InputStreamReader( stream )) {
				final String[] commands = extractor.extractCommands( reader );
				assertThat( commands, notNullValue() );
				assertThat( commands.length, is( 6 ) );

				// for Windows compatibility, System.lineSeparator() has to be used instead of just "\n"

				assertThat( commands[0], startsWith( "CREATE TABLE test_data" ) );

				assertThat( commands[1], is( "INSERT INTO test_data VALUES (1, 'sample')" ) );

				assertThat( commands[2], is( "DELETE   FROM test_data" ) );

				assertThat( commands[3], startsWith( "INSERT INTO test_data VALUES (2," ) );
				assertThat( commands[3], containsString( "-- line 2" ) );

				assertThat( commands[4], startsWith( "INSERT INTO test_data VALUES (3" ) );
				assertThat( commands[4], not( containsString( "third record" ) ) );

				assertThat( commands[5].replace( "\t", "" ), is( "INSERT INTO test_data VALUES (     4       , NULL     )" ) );
			}
		}
	}

	@Test
	public void testExtractionFromEmptyScript() throws IOException {
		StringReader reader = new StringReader( "" );
		final String[] commands = extractor.extractCommands( reader );
		assertThat( commands, notNullValue() );
		assertThat( commands.length, is( 0 ) );
	}

}
