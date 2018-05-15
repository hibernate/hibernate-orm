/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.foreignkeys;

import java.util.EnumSet;

import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
public class OneToManyForeignKeyGenerationTest extends BaseSchemaUnitTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class, UserSetting.class, Group.class };
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-10396")
	public void oneToManyTest(SchemaScope schemaScope) throws Exception {
		schemaScope.withSchemaExport( schemaExport ->
											  schemaExport
													  .setHaltOnError( true )
													  .setFormat( false )
													  .create( EnumSet.of( TargetType.SCRIPT ) ) );
		/*
		The generated SQL for the foreign keys should be:
		alter table GROUP add constraint FK_USER_GROUP foreign key (USER_ID) references USERS
		*/
		AlterTableChecker.checkAlterTableStatement(
				AlterTableChecker.createExpectedAlterTableStatement(
						"GROUP",
						"FK_USER_GROUP",
						"USER_ID",
						"USERS"
				)
				, getSqlScriptOutputFileLines() );
	}
}
