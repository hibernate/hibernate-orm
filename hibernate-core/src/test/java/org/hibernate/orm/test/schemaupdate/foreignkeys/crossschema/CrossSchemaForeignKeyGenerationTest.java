/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys.crossschema;

import org.hamcrest.MatcherAssert;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.orm.test.tool.schema.TargetDatabaseImpl;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.internal.GroupedSchemaMigratorImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.IndividuallySchemaMigratorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToStdout;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportSchemaCreation.class)
@ServiceRegistry(settings = @Setting(name = JAKARTA_HBM2DDL_CREATE_SCHEMAS, value = "true"))
@DomainModel(annotatedClasses = {SchemaOneEntity.class, SchemaTwoEntity.class})
public class CrossSchemaForeignKeyGenerationTest {
	@BeforeEach
	public void setUp(DomainModelScope modelScope) throws IOException {
		modelScope.getDomainModel().orderColumns( false );
		modelScope.getDomainModel().validate();
	}

	@Test
	@JiraKey(value = "HHH-10420")
	public void testSchemaExportForeignKeysAreGeneratedAfterAllTheTablesAreCreated(
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "fk-order.sql" );

		new SchemaExport().setHaltOnError( true )
				.setOutputFile( scriptFile.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT, TargetType.STDOUT ), modelScope.getDomainModel() );

		final List<String> sqlLines = Files.readAllLines( scriptFile.toPath(), Charset.defaultCharset() );
		MatcherAssert.assertThat( "Expected alter table SCHEMA1.Child add constraint but is : " + sqlLines.get( 4 ),
				sqlLines.get( sqlLines.size() - 1 ).startsWith( "alter table " ), is( true ) );
	}

	@Test
	@JiraKey(value = "HHH-10802")
	public void testSchemaUpdateDoesNotFailResolvingCrossSchemaForeignKey(
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "cross-schema.sql" );

		new SchemaExport()
				.setOutputFile( scriptFile.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );

		new SchemaUpdate().setHaltOnError( true )
				.setOutputFile( scriptFile.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );

		new SchemaExport().setHaltOnError( true )
				.setOutputFile( scriptFile.getAbsolutePath() )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
	}

	@Test
	@JiraKey(value = "HHH-10420")
	public void testSchemaMigrationForeignKeysAreGeneratedAfterAllTheTablesAreCreated(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope) throws Exception {
		final var metadata = modelScope.getDomainModel();

		final HibernateSchemaManagementTool tool = (HibernateSchemaManagementTool) registryScope
				.getRegistry()
				.requireService( SchemaManagementTool.class );

		final Map<String,Object> configurationValues = registryScope
				.getRegistry()
				.requireService( ConfigurationService.class )
				.getSettings();
		final ExecutionOptions options = new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return true;
			}

			@Override
			public Map<String,Object> getConfigurationValues() {
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
				ContributableMatcher.ALL,
				TargetDescriptorImpl.INSTANCE
		);

		new IndividuallySchemaMigratorImpl( tool, DefaultSchemaFilter.INSTANCE ).doMigration(
				metadata,
				options,
				ContributableMatcher.ALL,
				TargetDescriptorImpl.INSTANCE
		);

		new SchemaDropperImpl( tool ).doDrop(
				metadata,
				options,
				registryScope.getRegistry().requireService( JdbcEnvironment.class ).getDialect(),
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
				buildTargets( registryScope.getRegistry().requireService( JdbcServices.class ) )
		);
	}

	@Test
	@JiraKey(value = "HHH-10420")
	public void testImprovedSchemaMigrationForeignKeysAreGeneratedAfterAllTheTablesAreCreated(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope) throws Exception {
		final HibernateSchemaManagementTool tool = (HibernateSchemaManagementTool) registryScope
				.getRegistry()
				.requireService( SchemaManagementTool.class );

		final Map<String,Object> configurationValues = registryScope
				.getRegistry()
				.requireService( ConfigurationService.class )
				.getSettings();

		final ExecutionOptions options = new ExecutionOptions() {
			@Override
			public boolean shouldManageNamespaces() {
				return true;
			}

			@Override
			public Map<String,Object> getConfigurationValues() {
				return configurationValues;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return ExceptionHandlerLoggedImpl.INSTANCE;
			}
		};

		new GroupedSchemaMigratorImpl( tool, DefaultSchemaFilter.INSTANCE ).doMigration(
				modelScope.getDomainModel(),
				options,
				ContributableMatcher.ALL,
				TargetDescriptorImpl.INSTANCE
		);

		final var jdbcServices = registryScope.getRegistry().requireService( JdbcServices.class );

		new SchemaDropperImpl( tool ).doDrop(
				modelScope.getDomainModel(),
				options,
				jdbcServices.getDialect(),
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
				buildTargets( jdbcServices )
		);
	}

	public GenerationTarget[] buildTargets(JdbcServices jdbcServices) {
		return new GenerationTarget[] {
				new GenerationTargetToStdout(),
				new TargetDatabaseImpl( jdbcServices.getBootstrapJdbcConnectionAccess() )
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
