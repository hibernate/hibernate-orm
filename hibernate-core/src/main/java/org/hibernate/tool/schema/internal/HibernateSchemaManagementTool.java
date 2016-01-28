/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.sql.Connection;
import java.util.Map;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToScript;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToStdout;
import org.hibernate.tool.schema.internal.exec.JdbcConnectionAccessProvidedConnectionImpl;
import org.hibernate.tool.schema.internal.exec.JdbcConnectionContext;
import org.hibernate.tool.schema.internal.exec.JdbcConnectionContextNonSharedImpl;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.HBM2DDL_CONNECTION;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_DELIMITER;

/**
 * The standard Hibernate implementation for performing schema management.
 *
 * @author Steve Ebersole
 */
public class HibernateSchemaManagementTool implements SchemaManagementTool, ServiceRegistryAwareService {
	private static final Logger log = Logger.getLogger( HibernateSchemaManagementTool.class );

	private ServiceRegistry serviceRegistry;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public SchemaCreator getSchemaCreator(Map options) {
		return new SchemaCreatorImpl( this, getSchemaFilterProvider( options ).getCreateFilter() );
	}

	@Override
	public SchemaDropper getSchemaDropper(Map options) {
		return new SchemaDropperImpl( this, getSchemaFilterProvider( options ).getDropFilter() );
	}

	@Override
	public SchemaMigrator getSchemaMigrator(Map options) {
		return new SchemaMigratorImpl( this, getSchemaFilterProvider( options ).getMigrateFilter() );
	}

	@Override
	public SchemaValidator getSchemaValidator(Map options) {
		return new SchemaValidatorImpl( this, getSchemaFilterProvider( options ).getValidateFilter() );
	}
	
	private SchemaFilterProvider getSchemaFilterProvider(Map options) {
		final Object configuredOption = (options == null)
				? null
				: options.get( AvailableSettings.HBM2DDL_FILTER_PROVIDER );
		return serviceRegistry.getService( StrategySelector.class ).resolveDefaultableStrategy(
				SchemaFilterProvider.class,
				configuredOption,
				DefaultSchemaFilterProvider.INSTANCE
		);
	}

	GenerationTarget[] buildGenerationTargets(
			TargetDescriptor targetDescriptor,
			JdbcContext jdbcContext,
			Map options,
			boolean needsAutoCommit) {
		final String scriptDelimiter = ConfigurationHelper.getString( HBM2DDL_DELIMITER, options );

		final GenerationTarget[] targets = new GenerationTarget[ targetDescriptor.getTargetTypes().size() ];

		int index = 0;

		if ( targetDescriptor.getTargetTypes().contains( TargetType.STDOUT ) ) {
			targets[index] = new GenerationTargetToStdout( scriptDelimiter );
			index++;
		}

		if ( targetDescriptor.getTargetTypes().contains( TargetType.SCRIPT ) ) {
			if ( targetDescriptor.getScriptTargetOutput() == null ) {
				throw new SchemaManagementException( "Writing to script was requested, but no script file was specified" );
			}
			targets[index] = new GenerationTargetToScript( targetDescriptor.getScriptTargetOutput(), scriptDelimiter );
			index++;
		}

		if ( targetDescriptor.getTargetTypes().contains( TargetType.DATABASE ) ) {
			targets[index] = new GenerationTargetToDatabase(
					new JdbcConnectionContextNonSharedImpl(
							jdbcContext.getJdbcConnectionAccess(),
							jdbcContext.getSqlStatementLogger(),
							needsAutoCommit
					)
					, serviceRegistry.getService( JdbcEnvironment.class ).getSqlExceptionHelper()
			);

		}

		return targets;
	}

	GenerationTarget[] buildGenerationTargets(
			TargetDescriptor targetDescriptor,
			JdbcConnectionContext connectionContext,
			Map options) {
		final String scriptDelimiter = ConfigurationHelper.getString( HBM2DDL_DELIMITER, options );

		final GenerationTarget[] targets = new GenerationTarget[ targetDescriptor.getTargetTypes().size() ];

		int index = 0;

		if ( targetDescriptor.getTargetTypes().contains( TargetType.STDOUT ) ) {
			targets[index] = new GenerationTargetToStdout( scriptDelimiter );
			index++;
		}

		if ( targetDescriptor.getTargetTypes().contains( TargetType.SCRIPT ) ) {
			if ( targetDescriptor.getScriptTargetOutput() == null ) {
				throw new SchemaManagementException( "Writing to script was requested, but no script file was specified" );
			}
			targets[index] = new GenerationTargetToScript( targetDescriptor.getScriptTargetOutput(), scriptDelimiter );
			index++;
		}

		if ( targetDescriptor.getTargetTypes().contains( TargetType.DATABASE ) ) {
			targets[index] = new GenerationTargetToDatabase( connectionContext
					, serviceRegistry.getService( JdbcEnvironment.class ).getSqlExceptionHelper() );
		}

		return targets;
	}

	public JdbcContext resolveJdbcContext(Map configurationValues) {
		final JdbcContextBuilder jdbcContextBuilder = new JdbcContextBuilder( serviceRegistry );

		// see if a specific connection has been provided
		final Connection providedConnection = (Connection) configurationValues.get( HBM2DDL_CONNECTION );
		if ( providedConnection != null ) {
			jdbcContextBuilder.jdbcConnectionAccess = new JdbcConnectionAccessProvidedConnectionImpl( providedConnection );
		}

		// see if a specific Dialect override has been provided...
		final String explicitDbName = (String) configurationValues.get( AvailableSettings.HBM2DDL_DB_NAME );
		if ( StringHelper.isNotEmpty( explicitDbName ) ) {
			final String explicitDbMajor = (String) configurationValues.get( AvailableSettings.HBM2DDL_DB_MAJOR_VERSION );
			final String explicitDbMinor = (String) configurationValues.get( AvailableSettings.HBM2DDL_DB_MINOR_VERSION );

			final Dialect indicatedDialect = serviceRegistry.getService( DialectResolver.class ).resolveDialect(
					new DialectResolutionInfo() {
						@Override
						public String getDatabaseName() {
							return explicitDbName;
						}

						@Override
						public int getDatabaseMajorVersion() {
							return StringHelper.isEmpty( explicitDbMajor )
									? NO_VERSION
									: Integer.parseInt( explicitDbMajor );
						}

						@Override
						public int getDatabaseMinorVersion() {
							return StringHelper.isEmpty( explicitDbMinor )
									? NO_VERSION
									: Integer.parseInt( explicitDbMinor );
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
					}
			);

			if ( indicatedDialect == null ) {
				log.debugf(
						"Unable to resolve indicated Dialect resolution info (%s, %s, %s)",
						explicitDbName,
						explicitDbMajor,
						explicitDbMinor
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
		private final SqlStatementLogger sqlStatementLogger;
		private JdbcConnectionAccess jdbcConnectionAccess;
		private Dialect dialect;

		public JdbcContextBuilder(ServiceRegistry serviceRegistry) {
			final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
			this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
			this.dialect = jdbcServices.getJdbcEnvironment().getDialect();
			this.jdbcConnectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();
		}

		public JdbcContext buildJdbcContext() {
			return new JdbcContextImpl( jdbcConnectionAccess, dialect, sqlStatementLogger );
		}
	}

	public static class JdbcContextImpl implements JdbcContext {
		private final JdbcConnectionAccess jdbcConnectionAccess;
		private final Dialect dialect;
		private final SqlStatementLogger sqlStatementLogger;

		private JdbcContextImpl(
				JdbcConnectionAccess jdbcConnectionAccess,
				Dialect dialect,
				SqlStatementLogger sqlStatementLogger) {
			this.jdbcConnectionAccess = jdbcConnectionAccess;
			this.dialect = dialect;
			this.sqlStatementLogger = sqlStatementLogger;
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
	}
}
