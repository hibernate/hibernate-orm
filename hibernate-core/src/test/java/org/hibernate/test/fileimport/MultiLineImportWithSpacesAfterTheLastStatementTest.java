/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fileimport;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-14249")
public class MultiLineImportWithSpacesAfterTheLastStatementTest {
	public static final String IMPORT_FILE = "org/hibernate/test/fileimport/multi-line-statements-with-spaces-after-last-delimiter.sql";

	private final MultipleLinesSqlCommandExtractor extractor = new MultipleLinesSqlCommandExtractor();

	@Test
	public void testExtraction() throws Exception {
		final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

		try (final InputStream stream = classLoader.getResourceAsStream( IMPORT_FILE )) {
			assertThat( stream, notNullValue() );
			try (final InputStreamReader reader = new InputStreamReader( stream )) {
				final String[] commands = extractor.extractCommands( reader );
				assertThat( commands, notNullValue() );
				assertThat( commands.length, is( 3 ) );
			}
		}
	}
}
