/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import java.util.EnumSet;
import java.util.List;

import org.hibernate.orm.test.schemaupdate.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */

public class OneToOneForeignKeyGenerationTest extends BaseSchemaUnitTestCase {

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

	@Test
	@TestForIssue(jiraKey = "HHH-9591")
	public void oneToOneTest() throws Exception {
		createSchemaExport()
				.setHaltOnError( true )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ) );

		/*
		The generated SQL for the foreign keys should be:
		alter table USERS add constraint FK_TO_USER_SETTING foreign key (USER_SETTING_ID) references USER_SETTING
		alter table USER_SETTING add constraint FK_TO_USER foreign key (USERS_ID) references USERS
		*/
		final List<String> sqlScriptOutputFileLines = getSqlScriptOutputFileLines();
		AlterTableChecker.checkAlterTableStatement(
				AlterTableChecker.createExpectedAlterTableStatement(
						"USERS",
						"FK_TO_USER_SETTING",
						"USER_SETTING_ID",
						"USER_SETTING"
				)
				, sqlScriptOutputFileLines );

		AlterTableChecker.checkAlterTableStatement(
				AlterTableChecker.createExpectedAlterTableStatement(
						"USER_SETTING",
						"FK_TO_USER",
						"USER_ID",
						"USERS"
				)
				, sqlScriptOutputFileLines );
	}
}
