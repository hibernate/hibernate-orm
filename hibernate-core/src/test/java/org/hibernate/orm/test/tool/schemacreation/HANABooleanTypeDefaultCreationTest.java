/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.tool.schemacreation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Jonathan Bregler
 * @author Andrea Boriero
 */
public class HANABooleanTypeDefaultCreationTest extends AbstractHANAStringAndBooleanFieldsCreationTest {

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-12132")
	public void testTargetScriptIsCreatedBooleanTypeDefault(SchemaScope scope) throws Exception {
		String fileContent = getSqlScriptOutputFileContent();
		Pattern fileContentPattern = Pattern.compile(
				"create( (column|row))? table test_entity \\(field varchar.+, b boolean.+, c varchar.+, lob clob" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat(
				"Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true )
		);
	}

}
