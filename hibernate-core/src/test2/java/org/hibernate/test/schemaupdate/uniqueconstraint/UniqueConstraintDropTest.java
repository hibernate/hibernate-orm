/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.uniqueconstraint;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.IndividuallySchemaMigratorImpl;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToFile;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class UniqueConstraintDropTest {
	private File output;
	private MetadataImplementor metadata;
	private StandardServiceRegistry ssr;
	private HibernateSchemaManagementTool tool;
	private ExecutionOptions options;

	@Before
	public void setUp() throws Exception {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.applySetting( Environment.FORMAT_SQL, "false" )
				.applySetting( Environment.SHOW_SQL, "true" )
				.build();
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addResource( "org/hibernate/test/schemaupdate/uniqueconstraint/TestEntity.hbm.xml" )
				.buildMetadata();
		metadata.validate();
		tool = (HibernateSchemaManagementTool) ssr.getService( SchemaManagementTool.class );

		final Map configurationValues = ssr.getService( ConfigurationService.class ).getSettings();
		options = new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return true;
			}

			@Override
			public Map getConfigurationValues() {
				return configurationValues;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return ExceptionHandlerLoggedImpl.INSTANCE;
			}
		};
	}

	@After
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11236")
	public void testUniqueConstraintIsGenerated() throws Exception {

		new IndividuallySchemaMigratorImpl( tool, DefaultSchemaFilter.INSTANCE )
				.doMigration(
						metadata,
						options,
						new TargetDescriptorImpl()
				);

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

	protected Dialect getDialect() {
		return ssr.getService( JdbcEnvironment.class ).getDialect();
	}

	private boolean checkDropConstraint(String tableName, String columnName) throws IOException {
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

	private boolean checkDropIndex(String tableName, String columnName) throws IOException {
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

	private boolean checkDB2DropIndex(String tableName, String columnName) throws IOException {
		boolean matches = false;
		String regex = "drop index " + tableName + ".uk_(.)*";
		return isMatching( matches, regex );
	}

	private boolean isMatching(boolean matches, String regex) throws IOException {
		final String fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase();
		final String[] split = fileContent.split( System.lineSeparator() );
		Pattern p = Pattern.compile( regex );
		for ( String line : split ) {
			final Matcher matcher = p.matcher( line );
			if ( matcher.matches() ) {
				matches = true;
			}
		}
		return matches;
	}

	private class TargetDescriptorImpl implements TargetDescriptor {
		public EnumSet<TargetType> getTargetTypes() {
			return EnumSet.of( TargetType.SCRIPT );
		}

		@Override
		public ScriptTargetOutput getScriptTargetOutput() {
			return new ScriptTargetOutputToFile( output, Charset.defaultCharset().name() );
		}
	}
}
