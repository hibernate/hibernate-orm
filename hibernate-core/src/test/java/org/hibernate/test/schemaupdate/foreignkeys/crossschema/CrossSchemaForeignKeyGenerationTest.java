/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys.crossschema;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.GroupedSchemaMigratorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.IndividuallySchemaMigratorImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToStdout;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.tool.schema.TargetDatabaseImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@RequiresDialectFeature( value = DialectChecks.SupportSchemaCreation.class)
public class CrossSchemaForeignKeyGenerationTest extends BaseUnitTestCase {
	private File output;
	private StandardServiceRegistry ssr;

	@Before
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = new StandardServiceRegistryBuilder().applySetting( AvailableSettings.HBM2DLL_CREATE_SCHEMAS, "true" )
				.build();
	}

	@After
	public void tearsDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10420")
	public void testSchemaExportForeignKeysAreGeneratedAfterAllTheTablesAreCreated() throws Exception {
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( SchemaOneEntity.class );
		metadataSources.addAnnotatedClass( SchemaTwoEntity.class );

		MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();

		new SchemaExport().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT, TargetType.STDOUT ), metadata );

		final List<String> sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );
		assertThat(
				"Expected alter table SCHEMA1.Child add constraint but is : " + sqlLines.get( 4 ),
				sqlLines.get( sqlLines.size() - 1 ).startsWith( "alter table " ),
				is( true )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10802")
	public void testSchemaUpdateDoesNotFailResolvingCrossSchemaForeignKey() throws Exception {
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( SchemaOneEntity.class );
		metadataSources.addAnnotatedClass( SchemaTwoEntity.class );

		MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();

		new SchemaExport()
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.DATABASE ), metadata );

		new SchemaUpdate().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE ), metadata );

		new SchemaExport().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10420")
	public void testSchemaMigrationForeignKeysAreGeneratedAfterAllTheTablesAreCreated() throws Exception {
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( SchemaOneEntity.class );
		metadataSources.addAnnotatedClass( SchemaTwoEntity.class );

		MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();

		final Database database = metadata.getDatabase();
		final HibernateSchemaManagementTool tool = (HibernateSchemaManagementTool) ssr.getService( SchemaManagementTool.class );

		final Map configurationValues = ssr.getService( ConfigurationService.class ).getSettings();
		final ExecutionOptions options = new ExecutionOptions() {
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

		new IndividuallySchemaMigratorImpl( tool, DefaultSchemaFilter.INSTANCE ).doMigration(
				metadata,
				options,
				TargetDescriptorImpl.INSTANCE
		);

		new SchemaDropperImpl( tool ).doDrop(
				metadata,
				options,
				ssr.getService( JdbcEnvironment.class ).getDialect(),
				new SourceDescriptor() {
					@Override
					public SourceType getSourceType() {
						return SourceType.METADATA;
					}

					@Override
					public ScriptSourceInput getScriptSourceInput() {
						return null;
					}
				},
				buildTargets()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10420")
	public void testImprovedSchemaMigrationForeignKeysAreGeneratedAfterAllTheTablesAreCreated() throws Exception {
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( SchemaOneEntity.class );
		metadataSources.addAnnotatedClass( SchemaTwoEntity.class );

		MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();

		final HibernateSchemaManagementTool tool = (HibernateSchemaManagementTool) ssr.getService( SchemaManagementTool.class );

		final Map configurationValues = ssr.getService( ConfigurationService.class ).getSettings();
		final ExecutionOptions options = new ExecutionOptions() {
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

		new GroupedSchemaMigratorImpl( tool, DefaultSchemaFilter.INSTANCE ).doMigration(
				metadata,
				options,
				TargetDescriptorImpl.INSTANCE
		);

		new SchemaDropperImpl( tool ).doDrop(
				metadata,
				options,
				ssr.getService( JdbcEnvironment.class ).getDialect(),
				new SourceDescriptor() {
					@Override
					public SourceType getSourceType() {
						return SourceType.METADATA;
					}

					@Override
					public ScriptSourceInput getScriptSourceInput() {
						return null;
					}
				},
				buildTargets()
		);
	}

	public GenerationTarget[] buildTargets() {
		return new GenerationTarget[] {
				new GenerationTargetToStdout(),
				new TargetDatabaseImpl( ssr.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess() )
		};
	}

	private static class TargetDescriptorImpl implements TargetDescriptor {

		public static final TargetDescriptorImpl INSTANCE = new TargetDescriptorImpl();

		@Override
		public EnumSet<TargetType> getTargetTypes() {
			return EnumSet.of( TargetType.DATABASE );
		}

		@Override
		public ScriptTargetOutput getScriptTargetOutput() {
			return null;
		}
	}
}
