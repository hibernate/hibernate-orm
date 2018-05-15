/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
@TestForIssue(jiraKey = "HHH-1122")
public class SchemaUpdateDelimiterTest extends BaseSchemaUnitTestCase {

	public static final String EXPECTED_DELIMITER = ";";

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
		return new Class[] { TestEntity.class };
	}

	@SchemaTest
	public void testSchemaUpdateApplyDelimiterToGeneratedSQL(SchemaScope scope) throws Exception {

		scope.withSchemaUpdate( schemaUpdate ->
										schemaUpdate
												.setHaltOnError( true )
												.setDelimiter( EXPECTED_DELIMITER )
												.setFormat( false )
												.execute( EnumSet.of( TargetType.SCRIPT ) ) );

		List<String> sqlLines = getSqlScriptOutputFileLines();
		for ( String line : sqlLines ) {
			assertThat(
					"The expected delimiter is not applied " + line,
					line.endsWith( EXPECTED_DELIMITER ),
					is( true )
			);
		}
	}

	@Entity
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		private String field;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}
}
