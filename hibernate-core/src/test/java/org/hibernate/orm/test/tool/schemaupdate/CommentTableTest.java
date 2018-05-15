/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.io.IOException;
import java.util.EnumSet;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Table;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrea Boriero
 */
public class CommentTableTest extends BaseSchemaUnitTestCase {
	private static final String TABLE_NAME = "TABLE_WITH_COMMENT";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TableWithComment.class };
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
	@TestForIssue(jiraKey = "HHH-10451")
	public void testCommentOnTableStatementIsGenerated(SchemaScope schemaScope) throws IOException {
		schemaScope.withSchemaExport( schemaExport -> schemaExport.create( EnumSet.of( TargetType.SCRIPT ) ) );

		boolean found = false;
		for ( String sqlStatement : getSqlScriptOutputFileLines() ) {
			if ( sqlStatement.toLowerCase()
					.equals( "comment on table " + TABLE_NAME.toLowerCase() + " is 'comment snippet'" ) ) {
				if ( getDialect().supportsCommentOn() ) {
					found = true;
				}
				else {
					fail( "Generated " + sqlStatement + "  statement, but Dialect does not support it" );
				}
			}
			if ( containsCommentInCreateTableStatement( sqlStatement ) ) {
				if ( getDialect().supportsCommentOn() && !getDialect().getTableComment( "comment snippet" )
						.equals( "" ) ) {
					fail( "Added comment on create table statement when Dialect support create comment on table statement" );
				}
				else {
					found = true;
				}
			}
		}

		assertThat( "Table Comment Statement not correctly generated", found, is( true ) );
	}

	private boolean containsCommentInCreateTableStatement(String sqlStatement) {
		return sqlStatement.toLowerCase().contains( "create table" ) && sqlStatement.toLowerCase()
				.contains( getDialect().getTableComment( "comment snippet" )
								   .toLowerCase() );
	}

	@Entity(name = "TableWithComment")
	@javax.persistence.Table(name = "TABLE_WITH_COMMENT")
	@Table(appliesTo = TABLE_NAME, comment = "comment snippet")
	public static class TableWithComment {

		@Id
		private int id;
	}
}
