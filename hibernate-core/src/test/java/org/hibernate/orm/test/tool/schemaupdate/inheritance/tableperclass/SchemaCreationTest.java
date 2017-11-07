/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.inheritance.tableperclass;

import java.util.EnumSet;
import java.util.List;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
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
public class SchemaCreationTest extends BaseSchemaUnitTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Element.class, Category.class };
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-10553")
	public void testUniqueConstraintIsCorrectlyGenerated(SchemaScope schemaScope) throws Exception {
		schemaScope.withSchemaExport( schemaExport ->
								  schemaExport
										  .setHaltOnError( true )
										  .setFormat( false ).create( EnumSet.of( TargetType.SCRIPT ) ) );

		final List<String> sqlLines = getSqlScriptOutputFileLines();

		assertThatUniqueConstrainedIsCreated( sqlLines );
	}

	private void assertThatUniqueConstrainedIsCreated(List<String> sqlLines) {
		boolean isUniqueConstraintCreated = false;
		for ( String statement : sqlLines ) {
			assertThat(
					"Should not try to create the unique constraint for the non existing table element",
					statement.toLowerCase().contains( "alter table element" ),
					is( false )
			);
			if ( getStandardServiceRegistry().getService( JdbcEnvironment.class ).getDialect() instanceof DB2Dialect ) {
				if ( statement.toLowerCase().startsWith( "create unique index" )
						&& statement.toLowerCase().contains( "category (code)" ) ) {
					isUniqueConstraintCreated = true;
				}
			}
			else {
				if ( statement.toLowerCase().startsWith( "alter table category add constraint" )
						&& statement.toLowerCase().contains( "unique (code)" ) ) {
					isUniqueConstraintCreated = true;
				}
			}
		}

		assertThat(
				"Unique constraint for table category is not created",
				isUniqueConstraintCreated,
				is( true )
		);
	}
}
