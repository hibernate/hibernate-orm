/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Koen Aers
 */
@RequiresDialect(dialectClass = H2Dialect.class, matchSubTypes = true)
@TestForIssue(jiraKey = "HHH-10158")
public class SchemaUpdateFormatterTest extends BaseSchemaUnitTestCase {

	private static final String AFTER_FORMAT =
			"\n\\s+create table test_entity \\(\n" +
					"\\s+field varchar\\(255\\) not null,\n" +
					"\\s+primary key \\(field\\)\n" +
					"\\s+\\).*?;\n";
	private static final String DELIMITER = ";";

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
	public void testSetFormat(SchemaScope scope) throws Exception {

		scope.withSchemaUpdate( schemaUpdate ->
										schemaUpdate
												.setHaltOnError( true )
												.setDelimiter( DELIMITER )
												.setFormat( true )
												.execute( EnumSet.of( TargetType.SCRIPT ) ) );

		String outputContent = getSqlScriptOutputFileContent();
		//Old Macs use \r as a new line delimiter
		outputContent = outputContent.replaceAll( "\r", "\n" );
		//On Windows, \r\n would become \n\n, so we eliminate duplicates
		outputContent = outputContent.replaceAll( "\n\n", "\n" );

		assertTrue( Pattern.compile( AFTER_FORMAT ).matcher( outputContent ).matches() );
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
