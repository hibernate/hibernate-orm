/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.grammar.importsql;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;
import org.hibernate.tool.schema.internal.script.SqlScriptCommandExtracter;

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
	private final String FIRST = "first statement";
	private final String SECOND = "second statement";
	private final String THIRD = "third statement";

	@Test
	public void testSimpleExtraction() {
		final String firstCommand = FIRST;
		final String secondCommand = SECOND;
		final String thirdCommand = THIRD;

		final String commands = firstCommand + "; " + secondCommand + "; " + thirdCommand + ";";
		final Reader reader = new BufferedReader( new StringReader( commands ) );

		final SqlScriptCommandExtracter extractor = new SqlScriptCommandExtracter();
		final String[] extractedCommands = extractor.extractCommands( reader );

		assertThat( extractedCommands.length, is( 3 ) );
		assertThat( extractedCommands[0], is( FIRST ) );
		assertThat( extractedCommands[1], is( SECOND ) );
		assertThat( extractedCommands[2], is( THIRD ) );
	}

	@Test
	public void testMultiLineExtraction() {
		final String firstCommand = "first \nstatement";
		final String secondCommand = "second \nstatement";
		final String thirdCommand = "third \nstatement";

		final String commands = firstCommand + "; " + secondCommand + "; " + thirdCommand + ";";
		final Reader reader = new BufferedReader( new StringReader( commands ) );

		final SqlScriptCommandExtracter extractor = new SqlScriptCommandExtracter();
		final String[] extractedCommands = extractor.extractCommands( reader );

		assertThat( extractedCommands.length, is( 3 ) );
		assertThat( extractedCommands[0], is( FIRST ) );
		assertThat( extractedCommands[1], is( SECOND ) );
		assertThat( extractedCommands[2], is( THIRD ) );
	}

	@Test
	public void testMultiLineCommentExtraction() {
		final String firstCommand = "first \nstatement";
		final String secondCommand = "second \nstatement";
		final String thirdCommand = "third \n/*\n;\n*/statement";

		final String commands = firstCommand + "; " + secondCommand + "; " + thirdCommand + ";";
		final Reader reader = new BufferedReader( new StringReader( commands ) );

		final SqlScriptCommandExtracter extractor = new SqlScriptCommandExtracter();
		final String[] extractedCommands = extractor.extractCommands( reader );

		assertThat( extractedCommands.length, is( 3 ) );
		assertThat( extractedCommands[0], is( FIRST ) );
		assertThat( extractedCommands[1], is( SECOND ) );
		assertThat( extractedCommands[2], is( THIRD ) );
	}
}
