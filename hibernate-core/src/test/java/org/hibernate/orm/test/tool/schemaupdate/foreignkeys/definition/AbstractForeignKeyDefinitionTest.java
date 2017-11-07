/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.foreignkeys.definition;

import java.util.EnumSet;

import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Vlad MIhalcea
 */

public abstract class AbstractForeignKeyDefinitionTest extends BaseSchemaUnitTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return true;
	}

	protected abstract Class<?>[] getAnnotatedClasses();

	protected abstract boolean validate(String fileContent);

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-10643")
	public void testForeignKeyDefinitionOverridesDefaultNamingStrategy(SchemaScope schemaScope)
			throws Exception {
		schemaScope.withSchemaExport( schemaExport -> schemaExport
				.setHaltOnError( true )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ) ) );

		final String fileContent = getSqlScriptOutputFileContent();
		assertThat( "Script file : " + fileContent, validate( fileContent ), is( true ) );
	}

}
