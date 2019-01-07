/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12106")
@RequiresDialect(value = SQLServerDialect.class)
public class SqlServerQuoteSchemaTest extends BaseSchemaUnitTestCase {

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, Boolean.TRUE.toString() );
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Override
	protected void beforeEach(SchemaScope scope) {
		try {
			executeSqlStatement( "DROP TABLE [my-schema].my_entity" );
		}
		catch (Exception ignore) {

		}
		try {
			executeSqlStatement( "DROP SCHEMA [my-schema]" );
		}
		catch (Exception ignore) {

		}
		try {
			executeSqlStatement( "CREATE SCHEMA [my-schema]" );
		}
		catch (Exception ignore) {

		}
	}

	@Override
	protected void afterEach(SchemaScope scope) {
		try {
			executeSqlStatement( "DROP SCHEMA [my-schema]" );
		}
		catch (Exception ignore) {
		}
	}

	@SchemaTest
	public void test(SchemaScope scope) throws Exception {

		scope.withSchemaUpdate(
				schemaUpdate -> schemaUpdate.setHaltOnError( true )
						.setDelimiter( ";" )
						.setFormat( true )
						.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ) ) );

		String fileContent = getSqlScriptOutputFileContent();
		Pattern fileContentPattern = Pattern.compile( "create table \\[my\\-schema\\]\\.\\[my_entity\\]" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat( fileContentMatcher.find(), is( true ) );

		scope = regenerateSchemaScope( new Class[] { MyEntityUpdated.class } );

		scope.withSchemaUpdate(
				schemaUpdate -> schemaUpdate.setHaltOnError( true )
						.setDelimiter( ";" )
						.setFormat( true )
						.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ) ) );

		fileContent = getSqlScriptOutputFileContent();
		fileContentPattern = Pattern.compile( "alter table \\[my\\-schema\\]\\.\\[my_entity\\]" );
		fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat( fileContentMatcher.find(), is( true ) );
	}


	@Entity(name = "MyEntity")
	@Table(name = "my_entity", schema = "my-schema")
	public static class MyEntity {
		@Id
		public Integer id;
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity", schema = "my-schema")
	public static class MyEntityUpdated {
		@Id
		public Integer id;

		private String title;
	}

}
