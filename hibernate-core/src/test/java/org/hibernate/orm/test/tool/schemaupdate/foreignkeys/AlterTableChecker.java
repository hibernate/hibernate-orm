/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.foreignkeys;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
public class AlterTableChecker {

	public static void checkAlterTableStatement(String expectedAlterTableStatement, List<String> sqlLines )
			throws Exception {
		boolean found = false;
		for ( String line : sqlLines ) {
			if ( line.contains( expectedAlterTableStatement ) ) {
				found = true;
				return;
			}
		}
		assertThat( "Expected alter table statement not found : " + expectedAlterTableStatement, found, is( true ) );
	}

	public static String createExpectedAlterTableStatement(
			String tableName,
			String fkConstraintName,
			String fkColumnName,
			String referenceTableName) {
		return "alter table " + tableName + " add constraint " + fkConstraintName + " foreign key (" + fkColumnName + ") references " + referenceTableName;
	}
}
