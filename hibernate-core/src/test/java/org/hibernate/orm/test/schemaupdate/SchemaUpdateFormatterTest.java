package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
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
