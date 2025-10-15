/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.JdbcMetadataAccessStrategy;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToScript;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToStdout;
import org.hibernate.tool.schema.internal.exec.ImprovedExtractionContextImpl;
import org.hibernate.tool.schema.internal.exec.JdbcConnectionAccessProvidedConnectionImpl;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ExtractionTool;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.SchemaPopulator;
import org.hibernate.tool.schema.spi.SchemaTruncator;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.hibernate.tool.schema.spi.GeneratorSynchronizer;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.util.Map;

import static org.hibernate.cfg.JdbcSettings.DIALECT_DB_MAJOR_VERSION;
import static org.hibernate.cfg.JdbcSettings.DIALECT_DB_MINOR_VERSION;
import static org.hibernate.cfg.JdbcSettings.DIALECT_DB_NAME;
import static org.hibernate.cfg.JdbcSettings.DIALECT_DB_VERSION;
import static org.hibernate.cfg.JdbcSettings.HBM2DDL_CONNECTION;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_HBM2DDL_CONNECTION;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_HBM2DDL_DB_MAJOR_VERSION;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_HBM2DDL_DB_MINOR_VERSION;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_HBM2DDL_DB_NAME;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_HBM2DDL_DB_VERSION;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_DELIMITER;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_FILTER_PROVIDER;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;

/**
 * The standard Hibernate implementation of {@link SchemaManagementTool}
 * for performing schema management.
 *
 * @author Steve Ebersole
 */
public class HibernateSchemaManagementTool implements SchemaManagementTool, ServiceRegistryAwareService {
	private static final Logger LOG = Logger.getLogger( HibernateSchemaManagementTool.class );

	private ServiceRegistry serviceRegistry;
	private GenerationTarget customTarget;

	public HibernateSchemaManagementTool() {
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public SchemaCreator getSchemaCreator(Map<String,Object> options) {
		return new SchemaCreatorImpl( this, getSchemaFilterProvider( options ).getCreateFilter() );
	}

	@Override
	public SchemaDropper getSchemaDropper(Map<String,Object> options) {
		return new SchemaDropperImpl( this, getSchemaFilterProvider( options ).getDropFilter() );
	}

	@Override
	public SchemaTruncator getSchemaTruncator(Map<String,Object> options) {
		return new SchemaTruncatorImpl( this, getSchemaFilterProvider( options ).getTruncatorFilter() );
	}

	@Override
	public SchemaPopulator getSchemaPopulator(Map<String, Object> options) {
		return new SchemaPopulatorImpl( this );
	}

	@Override
	public GeneratorSynchronizer getSequenceSynchronizer(Map<String, Object> options) {
		return new GeneratorSynchronizerImpl( this, getSchemaFilterProvider( options ).getMigrateFilter() );
	}

	@Override
	public SchemaMigrator getSchemaMigrator(Map<String,Object> options) {
		final var migrateFilter = getSchemaFilterProvider( options ).getMigrateFilter();
		return JdbcMetadataAccessStrategy.interpretSetting( options ) == JdbcMetadataAccessStrategy.GROUPED
				? new GroupedSchemaMigratorImpl( this, migrateFilter )
				: new IndividuallySchemaMigratorImpl( this, migrateFilter );
	}

	@Override
	public SchemaValidator getSchemaValidator(Map<String,Object> options) {
		final var validateFilter = getSchemaFilterProvider( options ).getValidateFilter();
		return JdbcMetadataAccessStrategy.interpretSetting( options ) == JdbcMetadataAccessStrategy.GROUPED
				? new GroupedSchemaValidatorImpl( this, validateFilter )
				: new IndividuallySchemaValidatorImpl( this, validateFilter );
	}

	private SchemaFilterProvider getSchemaFilterProvider(Map<String,Object> options) {
		return serviceRegistry.requireService( StrategySelector.class )
				.resolveDefaultableStrategy( SchemaFilterProvider.class,
						options == null ? null : options.get( HBM2DDL_FILTER_PROVIDER ),
						DefaultSchemaFilterProvider.INSTANCE );
	}

	@Override
	public void setCustomDatabaseGenerationTarget(GenerationTarget generationTarget) {
		this.customTarget = generationTarget;
	}

	@Override
	public ExtractionTool getExtractionTool() {
		return HibernateExtractionTool.INSTANCE;
	}

	GenerationTarget getCustomDatabaseGenerationTarget() {
		return customTarget;
	}

	@Override
	public GenerationTarget[] buildGenerationTargets(
			TargetDescriptor targetDescriptor,
			JdbcContext jdbcContext,
			Map<String, Object> options,
			boolean needsAutoCommit) {
		final String scriptDelimiter = getString( HBM2DDL_DELIMITER, options, ";" );

		final var targets = new GenerationTarget[ targetDescriptor.getTargetTypes().size() ];

		int index = 0;

		if ( targetDescriptor.getTargetTypes().contains( TargetType.STDOUT ) ) {
			targets[index] = buildStdoutTarget( scriptDelimiter );
			index++;
		}

		if ( targetDescriptor.getTargetTypes().contains( TargetType.SCRIPT ) ) {
			if ( targetDescriptor.getScriptTargetOutput() == null ) {
				throw new SchemaManagementException( "Writing to script was requested, but no script file was specified" );
			}
			targets[index] = buildScriptTarget( targetDescriptor, scriptDelimiter );
			index++;
		}

		if ( targetDescriptor.getTargetTypes().contains( TargetType.DATABASE ) ) {
			targets[index] = customTarget == null
					? buildDatabaseTarget( jdbcContext, needsAutoCommit )
					: customTarget;
			index++;
		}

		return targets;
	}

	protected GenerationTarget buildStdoutTarget(String scriptDelimiter) {
		return new GenerationTargetToStdout( scriptDelimiter );
	}

	protected GenerationTarget buildScriptTarget(TargetDescriptor targetDescriptor, String scriptDelimiter) {
		return new GenerationTargetToScript( targetDescriptor.getScriptTargetOutput(), scriptDelimiter );
	}

	protected GenerationTarget buildDatabaseTarget(JdbcContext jdbcContext, boolean needsAutoCommit) {
		return new GenerationTargetToDatabase( getDdlTransactionIsolator( jdbcContext ), true, needsAutoCommit );
	}

	GenerationTarget[] buildGenerationTargets(
			TargetDescriptor targetDescriptor,
			DdlTransactionIsolator ddlTransactionIsolator,
			Map<String,Object> options) {
		final String scriptDelimiter = getString( HBM2DDL_DELIMITER, options, ";" );

		final var targets = new GenerationTarget[ targetDescriptor.getTargetTypes().size() ];

		int index = 0;

		if ( targetDescriptor.getTargetTypes().contains( TargetType.STDOUT ) ) {
			targets[index] = buildStdoutTarget( scriptDelimiter );
			index++;
		}

		if ( targetDescriptor.getTargetTypes().contains( TargetType.SCRIPT ) ) {
			if ( targetDescriptor.getScriptTargetOutput() == null ) {
				throw new SchemaManagementException( "Writing to script was requested, but no script file was specified" );
			}
			targets[index] = buildScriptTarget( targetDescriptor, scriptDelimiter );
			index++;
		}

		if ( targetDescriptor.getTargetTypes().contains( TargetType.DATABASE ) ) {
			targets[index] = customTarget == null
					? new GenerationTargetToDatabase( ddlTransactionIsolator, false )
					: customTarget;
			index++;
		}

		return targets;
	}

	public DdlTransactionIsolator getDdlTransactionIsolator(JdbcContext jdbcContext) {
		if ( jdbcContext.getJdbcConnectionAccess() instanceof JdbcConnectionAccessProvidedConnectionImpl ) {
			return new DdlTransactionIsolatorProvidedConnectionImpl( jdbcContext );
		}
		return serviceRegistry.requireService( TransactionCoordinatorBuilder.class )
				.buildDdlTransactionIsolator( jdbcContext );
	}

	public JdbcContext resolveJdbcContext(Map<String,Object> configurationValues) {
		final var jdbcContextBuilder = new JdbcContextBuilder( serviceRegistry );

		// see if a specific connection has been provided
		final var providedConnection = (Connection) coalesceSuppliedValues(
				() -> configurationValues.get( JAKARTA_HBM2DDL_CONNECTION ),
				() -> {
					final Object value = configurationValues.get( HBM2DDL_CONNECTION );
					if ( value != null ) {
						DEPRECATION_LOGGER.deprecatedSetting( HBM2DDL_CONNECTION, JAKARTA_HBM2DDL_CONNECTION );
					}
					return value;
				}
		);
		if ( providedConnection != null ) {
			jdbcContextBuilder.jdbcConnectionAccess =
					new JdbcConnectionAccessProvidedConnectionImpl( providedConnection );
		}

		// see if a specific Dialect override has been provided through database name, version, etc
		final String dbName = (String) coalesceSuppliedValues(
				() -> configurationValues.get( JAKARTA_HBM2DDL_DB_NAME ),
				() -> {
					final String name = (String) configurationValues.get( DIALECT_DB_NAME );
					if ( isNotEmpty( name ) ) {
						DEPRECATION_LOGGER.deprecatedSetting( DIALECT_DB_NAME, JAKARTA_HBM2DDL_DB_NAME );
					}
					return name;
				}
		);
		if ( dbName != null ) {
			final String dbVersion = (String) coalesceSuppliedValues(
					() -> configurationValues.get( JAKARTA_HBM2DDL_DB_VERSION ),
					() -> {
						final String name = (String) configurationValues.get( DIALECT_DB_VERSION );
						if ( isNotEmpty( name ) ) {
							DEPRECATION_LOGGER.deprecatedSetting( DIALECT_DB_VERSION, JAKARTA_HBM2DDL_DB_VERSION );
						}
						return name;
					}
			);

			final String dbMajor = (String) coalesceSuppliedValues(
					() -> configurationValues.get( JAKARTA_HBM2DDL_DB_MAJOR_VERSION ),
					() -> {
						final String name = (String) configurationValues.get( DIALECT_DB_MAJOR_VERSION );
						if ( isNotEmpty( name ) ) {
							DEPRECATION_LOGGER.deprecatedSetting( DIALECT_DB_MAJOR_VERSION, JAKARTA_HBM2DDL_DB_MAJOR_VERSION );
						}
						return name;
					}
			);

			final String dbMinor = (String) coalesceSuppliedValues(
					() -> configurationValues.get( JAKARTA_HBM2DDL_DB_MINOR_VERSION ),
					() -> {
						final String name = (String) configurationValues.get( DIALECT_DB_MINOR_VERSION );
						if ( isNotEmpty( name ) ) {
							DEPRECATION_LOGGER.deprecatedSetting( DIALECT_DB_MINOR_VERSION, JAKARTA_HBM2DDL_DB_MINOR_VERSION );
						}
						return name;
					}
			);

			final Dialect indicatedDialect = serviceRegistry.requireService( DialectResolver.class ).resolveDialect(
					new DialectResolutionInfo() {
						@Override
						public String getDatabaseName() {
							return dbName;
						}

						@Override
						public String getDatabaseVersion() {
							return dbVersion == null
									? String.valueOf( NO_VERSION ) :
									dbVersion;
						}

						@Override
						public int getDatabaseMajorVersion() {
							return isEmpty( dbMajor )
									? NO_VERSION
									: Integer.parseInt( dbMajor );
						}

						@Override
						public int getDatabaseMinorVersion() {
							return isEmpty( dbMinor )
									? NO_VERSION
									: Integer.parseInt( dbMinor );
						}

						@Override
						public String getDriverName() {
							return null;
						}

						@Override
						public int getDriverMajorVersion() {
							return NO_VERSION;
						}

						@Override
						public int getDriverMinorVersion() {
							return NO_VERSION;
						}

						@Override
						public String getSQLKeywords() {
							return "";
						}

						@Override
						public Map<String, Object> getConfigurationValues() {
							return configurationValues;
						}
					}
			);

			if ( indicatedDialect == null ) {
				LOG.debugf(
						"Unable to resolve indicated Dialect resolution info (%s, %s, %s)",
						dbName,
						dbMajor,
						dbMinor
				);
			}
			else {
				jdbcContextBuilder.dialect = indicatedDialect;
			}
		}

		return jdbcContextBuilder.buildJdbcContext();
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	private static class JdbcContextBuilder {
		private final ServiceRegistry serviceRegistry;
		private final SqlStatementLogger sqlStatementLogger;
		private final SqlExceptionHelper sqlExceptionHelper;

		private JdbcConnectionAccess jdbcConnectionAccess;
		private Dialect dialect;

		public JdbcContextBuilder(ServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;
			final JdbcServices jdbcServices = serviceRegistry.requireService( JdbcServices.class );
			this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
			this.sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();

			this.dialect = jdbcServices.getJdbcEnvironment().getDialect();
			this.jdbcConnectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();
		}

		public JdbcContext buildJdbcContext() {
			return new JdbcContextImpl( jdbcConnectionAccess, dialect, sqlStatementLogger, sqlExceptionHelper, serviceRegistry );
		}
	}

	public static class JdbcContextImpl implements JdbcContext {
		private final JdbcConnectionAccess jdbcConnectionAccess;
		private final Dialect dialect;
		private final SqlStatementLogger sqlStatementLogger;
		private final SqlExceptionHelper sqlExceptionHelper;
		private final ServiceRegistry serviceRegistry;

		private JdbcContextImpl(
				JdbcConnectionAccess jdbcConnectionAccess,
				Dialect dialect,
				SqlStatementLogger sqlStatementLogger,
				SqlExceptionHelper sqlExceptionHelper,
				ServiceRegistry serviceRegistry) {
			this.jdbcConnectionAccess = jdbcConnectionAccess;
			this.dialect = dialect;
			this.sqlStatementLogger = sqlStatementLogger;
			this.sqlExceptionHelper = sqlExceptionHelper;
			this.serviceRegistry = serviceRegistry;
		}

		@Override
		public JdbcConnectionAccess getJdbcConnectionAccess() {
			return jdbcConnectionAccess;
		}

		@Override
		public Dialect getDialect() {
			return dialect;
		}

		@Override
		public SqlStatementLogger getSqlStatementLogger() {
			return sqlStatementLogger;
		}

		@Override
		public SqlExceptionHelper getSqlExceptionHelper() {
			return sqlExceptionHelper;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}
	}

	private static class HibernateExtractionTool implements ExtractionTool {

		private static final HibernateExtractionTool INSTANCE = new HibernateExtractionTool();

		private HibernateExtractionTool() {
		}

		@Override
		public ExtractionContext createExtractionContext(
				ServiceRegistry serviceRegistry,
				JdbcEnvironment jdbcEnvironment,
				SqlStringGenerationContext context,
				DdlTransactionIsolator ddlTransactionIsolator,
				ExtractionContext.DatabaseObjectAccess databaseObjectAccess) {
			return new ImprovedExtractionContextImpl(
					serviceRegistry,
					jdbcEnvironment,
					context,
					ddlTransactionIsolator,
					databaseObjectAccess
			);
		}

		@Override
		public InformationExtractor createInformationExtractor(ExtractionContext extractionContext) {
			return extractionContext.getJdbcEnvironment().getDialect().getInformationExtractor( extractionContext );
		}
	}
}
