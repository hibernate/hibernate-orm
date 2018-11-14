/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.uniqueconstraint;

import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
public class UniqueConstraintDropTest extends BaseSchemaUnitTestCase {
	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( Environment.HBM2DDL_AUTO, "none" );
		serviceRegistryBuilder.applySetting( Environment.FORMAT_SQL, "false" );
	}

	@Override
	protected String[] getHmbMappingFiles() {
		return new String[] { "tool/schemaupdate/uniqueconstraint/TestEntity.hbm.xml" };
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-11236")
	public void testUniqueConstraintIsDropped(SchemaScope scope) throws Exception {

		scope.withSchemaMigrator ( schemaMigrator ->
				schemaMigrator.doMigration(
						new TestExecutionOptions(),
						new TestTargetDescriptor()
				)  );

		if ( getDialect() instanceof MySQLDialect ) {
			assertThat(
					"The test_entity_item table unique constraint has not been dropped",
					checkDropIndex( "test_entity_item", "item" ),
					is( true )
			);
		}
		else if ( getDialect() instanceof DB2Dialect ) {
			checkDB2DropIndex( "test_entity_item", "item" );
		}
		else {
			assertThat(
					"The test_entity_item table unique constraint has not been dropped",
					checkDropConstraint( "test_entity_item", "item" ),
					is( true )
			);
		}
	}

	private boolean checkDropConstraint(String tableName, String columnName) throws Exception {
		boolean matches = false;
		String regex = getDialect().getAlterTableString( tableName ) + " drop constraint";

		if ( getDialect().supportsIfExistsBeforeConstraintName() ) {
			regex += " if exists";
		}
		regex += " uk_(.)*";
		if ( getDialect().supportsIfExistsAfterConstraintName() ) {
			regex += " if exists";
		}

		return isMatching( matches, regex );
	}

	private boolean checkDropIndex(String tableName, String columnName) throws Exception {
		boolean matches = false;
		String regex = "alter table " + tableName + " drop index";

		if ( getDialect().supportsIfExistsBeforeConstraintName() ) {
			regex += " if exists";
		}
		regex += " uk_(.)*";
		if ( getDialect().supportsIfExistsAfterConstraintName() ) {
			regex += " if exists";
		}

		return isMatching( matches, regex );
	}

	private boolean checkDB2DropIndex(String tableName, String columnName) throws Exception {
		boolean matches = false;
		String regex = "drop index " + tableName + ".uk_(.)*";
		return isMatching( matches, regex );
	}

	private boolean isMatching(boolean matches, String regex) throws Exception {
		final String[] sqlGeneratedStatements = getSqlScriptOutputFileContent().toLowerCase()
				.split( System.lineSeparator() );
		Pattern p = Pattern.compile( regex );
		for ( String statement : sqlGeneratedStatements ) {
			final Matcher matcher = p.matcher( statement );
			if ( matcher.matches() ) {
				matches = true;
			}
		}
		return matches;
	}

	class TestExecutionOptions implements ExecutionOptions {
		@Override
		public boolean shouldManageNamespaces() {
			return true;
		}

		@Override
		public Map getConfigurationValues() {
			return getStandardServiceRegistry().getService( ConfigurationService.class ).getSettings();
		}

		@Override
		public ExceptionHandler getExceptionHandler() {
			return ExceptionHandlerLoggedImpl.INSTANCE;
		}
	}

	class TestTargetDescriptor implements TargetDescriptor {

		@Override
		public EnumSet<TargetType> getTargetTypes() {
			return EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE );
		}

		@Override
		public ScriptTargetOutput getScriptTargetOutput() {
			return getScriptTargetOutputToFile();
		}
	}
}
