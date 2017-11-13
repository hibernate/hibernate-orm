/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;

import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10197")
public class QuotedTableNameWithForeignKeysSchemaUpdateHbmTest extends BaseSchemaUnitTestCase {

	@Override
	protected String[] getHmbMappingFiles() {
		return new String[] { "tool/schemaupdate/UserGroup.hbm.xml" };
	}

	@Override
	protected void beforeEach(SchemaScope scope) {
		scope.withSchemaUpdate( schemaUpdate -> {
			schemaUpdate.execute( EnumSet.of( TargetType.DATABASE ) );
			assertThat(
					"An unexpected Exception occurred during the database schema update",
					schemaUpdate.getExceptions().size(),
					is( 0 )
			);
		} );
	}

	@SchemaTest
	public void testUpdateExistingSchema(SchemaScope scope) {
		scope.withSchemaUpdate( schemaUpdate -> {
			schemaUpdate.execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ) );
			assertThat(
					"An unexpected Exception occurred during the database schema update",
					schemaUpdate.getExceptions().size(),
					is( 0 )
			);
		} );
	}
}
