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
public class OneToManyWithJoinTableForeignKeyGenerationTest extends BaseSchemaUnitTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Phone.class };
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
	@TestForIssue(jiraKey = "HHH-10385")
	public void oneToManyWithJoinTableTest() throws Exception {
		createSchemaExport()
				.setHaltOnError( true )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ) );
		/*
			The generated SQL for the foreign keys should be:
            alter table PERSON_PHONE add constraint PERSON_ID_FK foreign key (PERSON_ID) references PERSON
            alter table PERSON_PHONE add constraint PHONE_ID_FK foreign key (PHONE_ID) references PHONE
        */
		final List<String> sqlScriptOutputFileLines = getSqlScriptOutputFileLines();
		AlterTableChecker.checkAlterTableStatement(
				AlterTableChecker.createExpectedAlterTableStatement(
						"PERSON_PHONE",
						"PERSON_ID_FK",
						"PERSON_ID",
						"PERSON"
				)
				, sqlScriptOutputFileLines );

		AlterTableChecker.checkAlterTableStatement(
				AlterTableChecker.createExpectedAlterTableStatement(
						"PERSON_PHONE",
						"PHONE_ID_FK",
						"PHONE_ID",
						"PHONE"
				)
				, sqlScriptOutputFileLines );
	}
}
