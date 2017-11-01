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
public class ManyToManyForeignKeyGenerationTest extends BaseSchemaUnitTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Project.class, Employee.class };
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
	@TestForIssue(jiraKey = "HHH-10386")
	public void oneToManyWithJoinTableTest() throws Exception {
		createSchemaExport()
				.setHaltOnError( true )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ) );
		/*
			The generated SQL for the foreign keys should be:
            alter table EMPLOYEE_PROJECT add constraint FK_EMPLOYEE foreign key (EMPLOYEE_ID) references EMPLOYEE
            alter table EMPLOYEE_PROJECT add constraint FK_PROJECT foreign key (PROJECT_ID) references PROJECT
        */
		final List<String> sqlScriptOutputFileLines = getSqlScriptOutputFileLines();
		AlterTableChecker.checkAlterTableStatement(
				AlterTableChecker.createExpectedAlterTableStatement(
						"EMPLOYEE_PROJECT",
						"FK_EMPLOYEE",
						"EMPLOYEE_ID",
						"EMPLOYEE"
				)
				, sqlScriptOutputFileLines );

		AlterTableChecker.checkAlterTableStatement(
				AlterTableChecker.createExpectedAlterTableStatement(
						"EMPLOYEE_PROJECT",
						"FK_PROJECT",
						"PROJECT_ID",
						"PROJECT"
				)
				, sqlScriptOutputFileLines );
	}
}
