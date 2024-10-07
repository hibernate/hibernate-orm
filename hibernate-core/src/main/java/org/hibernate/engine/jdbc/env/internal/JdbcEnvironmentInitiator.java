/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.engine.jdbc.internal.JdbcServicesImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.internal.EmptyEventManager;
import org.hibernate.event.spi.EventManager;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.jpa.internal.MutableJpaComplianceImpl;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.resource.jdbc.spi.JdbcObserver;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.CONNECTION_HANDLING;
import static org.hibernate.cfg.AvailableSettings.DIALECT_DB_MAJOR_VERSION;
import static org.hibernate.cfg.AvailableSettings.DIALECT_DB_MINOR_VERSION;
import static org.hibernate.cfg.AvailableSettings.DIALECT_DB_NAME;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DB_MAJOR_VERSION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DB_MINOR_VERSION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DB_NAME;
import static org.hibernate.cfg.AvailableSettings.JTA_TRACK_BY_THREAD;
import static org.hibernate.cfg.AvailableSettings.PREFER_USER_TRANSACTION;
import static org.hibernate.cfg.JdbcSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.JdbcSettings.DIALECT_DB_VERSION;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_HBM2DDL_DB_VERSION;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import static org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl.isMultiTenancyEnabled;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBooleanWrapper;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInteger;

/**
 * @author Steve Ebersole
 */
public class JdbcEnvironmentInitiator implements StandardServiceInitiator<JdbcEnvironment> {
	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			JdbcEnvironmentInitiator.class.getName()
	);

	public static final JdbcEnvironmentInitiator INSTANCE = new JdbcEnvironmentInitiator();

	/**
	 * @deprecated This setting was never a documented feature of Hibernate,
	 *			 is not supported, and will be removed.
	 */
	@Deprecated(since="6", forRemoval = true)
	private static final String USE_JDBC_METADATA_DEFAULTS = "hibernate.temp.use_jdbc_metadata_defaults";

	@Override
	public Class<JdbcEnvironment> getServiceInitiated() {
		return JdbcEnvironment.class;
	}

	@Override
	public JdbcEnvironment initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final DialectFactory dialectFactory = registry.requireService( DialectFactory.class );

		final String explicitDatabaseName = getExplicitDatabaseName( configurationValues );
		Integer explicitDatabaseMajorVersion = getExplicitDatabaseMajorVersion( configurationValues );
		Integer explicitDatabaseMinorVersion = getExplicitDatabaseMinorVersion( configurationValues );

		final String explicitDatabaseVersion =
				getExplicitDatabaseVersion( configurationValues, explicitDatabaseMajorVersion, explicitDatabaseMinorVersion );

		if ( explicitDatabaseMajorVersion == null && explicitDatabaseMinorVersion == null && explicitDatabaseVersion != null ) {
			final String[] parts = explicitDatabaseVersion.split( "\\." );
			try {
				final int potentialMajor = Integer.parseInt( parts[0] );
				if ( parts.length > 1 ) {
					explicitDatabaseMinorVersion = Integer.parseInt( parts[1] );
				}
				explicitDatabaseMajorVersion = potentialMajor;
			}
			catch (NumberFormatException e) {
				// Ignore
			}
		}

		final JdbcEnvironment jdbcEnvironment;
		DatabaseConnectionInfo databaseConnectionInfo;
		if ( allowJdbcMetadataAccess( configurationValues ) ) {
			jdbcEnvironment = getJdbcEnvironmentUsingJdbcMetadata(
					configurationValues,
					registry,
					dialectFactory,
					explicitDatabaseName,
					explicitDatabaseMajorVersion,
					explicitDatabaseMinorVersion,
					explicitDatabaseVersion);
			databaseConnectionInfo = buildDbInfo( registry, jdbcEnvironment.getDialect() );
		}
		else if ( explicitDialectConfiguration( explicitDatabaseName, configurationValues ) ) {
			jdbcEnvironment = getJdbcEnvironmentWithExplicitConfiguration(
					configurationValues,
					registry,
					dialectFactory,
					explicitDatabaseName,
					explicitDatabaseMajorVersion,
					explicitDatabaseMinorVersion,
					explicitDatabaseVersion
			);
			databaseConnectionInfo = buildDbInfo( configurationValues, jdbcEnvironment.getDialect() );
		}
		else {
			jdbcEnvironment = getJdbcEnvironmentWithDefaults( configurationValues, registry, dialectFactory );
			databaseConnectionInfo = buildDbInfo( configurationValues, jdbcEnvironment.getDialect() );
		}

		// Standardized DB info logging
		ConnectionInfoLogger.INSTANCE.logConnectionInfoDetails( databaseConnectionInfo.toInfoString() );

		return jdbcEnvironment;
	}

	private DatabaseConnectionInfo buildDbInfo(ServiceRegistryImplementor registry, Dialect dialect) {
		if ( !isMultiTenancyEnabled( registry ) ) {
			final ConnectionProvider cp = registry.requireService( ConnectionProvider.class );
			return cp.getDatabaseConnectionInfo( dialect );
		}
		else {
			final MultiTenantConnectionProvider<?> mcp = registry.requireService( MultiTenantConnectionProvider.class );
			return mcp.getDatabaseConnectionInfo( dialect );
		}
	}

	private DatabaseConnectionInfo buildDbInfo(Map<String, Object> configurationValues, Dialect dialect) {
		return new DatabaseConnectionInfoImpl( configurationValues, dialect );
	}

	private static JdbcEnvironmentImpl getJdbcEnvironmentWithDefaults(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry,
			DialectFactory dialectFactory) {
		final Dialect dialect = dialectFactory.buildDialect( configurationValues, null );
		return new JdbcEnvironmentImpl( registry, dialect );
	}

	private static JdbcEnvironmentImpl getJdbcEnvironmentWithExplicitConfiguration(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry,
			DialectFactory dialectFactory,
			String explicitDatabaseName,
			Integer explicitDatabaseMajorVersion,
			Integer explicitDatabaseMinorVersion,
			String explicitDatabaseVersion) {
		final DialectResolutionInfo dialectResolutionInfo = new DialectResolutionInfoImpl(
				null,
				explicitDatabaseName,
				explicitDatabaseVersion,
				explicitDatabaseMajorVersion != null ? explicitDatabaseMajorVersion : DatabaseVersion.NO_VERSION,
				explicitDatabaseMinorVersion != null ? explicitDatabaseMinorVersion : DatabaseVersion.NO_VERSION,
				DatabaseVersion.NO_VERSION,
				null,
				DatabaseVersion.NO_VERSION,
				DatabaseVersion.NO_VERSION,
				null,
				configurationValues
		);
		final Dialect dialect = dialectFactory.buildDialect( configurationValues, () -> dialectResolutionInfo );
		return new JdbcEnvironmentImpl( registry, dialect );
	}

	/**
	 * Determine whether we can access JDBC {@linkplain DatabaseMetaData metadata} based on
	 * the {@value JdbcSettings#ALLOW_METADATA_ON_BOOT} setting. The default is to allow access.
	 *
	 * @implNote Currently also looks for the deprecated {@value JdbcEnvironmentInitiator#USE_JDBC_METADATA_DEFAULTS} setting as a fallback.
	 *
	 * @see JdbcSettings#ALLOW_METADATA_ON_BOOT
	 */
	public static boolean allowJdbcMetadataAccess(Map<String, Object> configurationValues) {
		final Boolean allow = getBooleanWrapper( ALLOW_METADATA_ON_BOOT, configurationValues, null );
		if ( allow != null ) {
			return allow;
		}

		final Boolean use = getBooleanWrapper( USE_JDBC_METADATA_DEFAULTS, configurationValues, null );
		if ( use != null ) {
			DEPRECATION_LOGGER.deprecatedSetting( USE_JDBC_METADATA_DEFAULTS, ALLOW_METADATA_ON_BOOT );
			return use;
		}

		// allow by default
		return true;
	}

	private static String getExplicitDatabaseVersion(
			Map<String, Object> configurationValues,
			Integer configuredDatabaseMajorVersion,
			Integer configuredDatabaseMinorVersion) {
		return coalesceSuppliedValues(
				() -> (String) configurationValues.get( JAKARTA_HBM2DDL_DB_VERSION ),
				() -> {
					final Object value = configurationValues.get( DIALECT_DB_VERSION );
					if ( value != null ) {
						DEPRECATION_LOGGER.deprecatedSetting( DIALECT_DB_VERSION, JAKARTA_HBM2DDL_DB_VERSION );
					}
					return (String) value;
				}
				,
				() -> {
					if ( configuredDatabaseMajorVersion != null ) {
						return configuredDatabaseMinorVersion == null
								? configuredDatabaseMajorVersion.toString()
								: configuredDatabaseMajorVersion + "." + configuredDatabaseMinorVersion;
					}
					return null;
				}
		);
	}

	private static Integer getExplicitDatabaseMinorVersion(Map<String, Object> configurationValues) {
		return coalesceSuppliedValues(
				() -> getInteger( JAKARTA_HBM2DDL_DB_MINOR_VERSION, configurationValues ),
				() -> {
					final Integer value = getInteger( DIALECT_DB_MINOR_VERSION, configurationValues );
					if ( value != null ) {
						DEPRECATION_LOGGER.deprecatedSetting( DIALECT_DB_MINOR_VERSION, JAKARTA_HBM2DDL_DB_MINOR_VERSION );
					}
					return value;
				}
		);
	}

	private static Integer getExplicitDatabaseMajorVersion(Map<String, Object> configurationValues) {
		return coalesceSuppliedValues(
				() -> getInteger( JAKARTA_HBM2DDL_DB_MAJOR_VERSION, configurationValues ),
				() -> {
					final Integer value = getInteger( DIALECT_DB_MAJOR_VERSION, configurationValues );
					if ( value != null ) {
						DEPRECATION_LOGGER.deprecatedSetting( DIALECT_DB_MAJOR_VERSION, JAKARTA_HBM2DDL_DB_MAJOR_VERSION );
					}
					return value;
				}
		);
	}

	private static String getExplicitDatabaseName(Map<String, Object> configurationValues) {
		return coalesceSuppliedValues(
				() -> (String) configurationValues.get(JAKARTA_HBM2DDL_DB_NAME),
				() -> {
					final Object value = configurationValues.get( DIALECT_DB_NAME );
					if ( value != null ) {
						DEPRECATION_LOGGER.deprecatedSetting( DIALECT_DB_NAME, JAKARTA_HBM2DDL_DB_NAME );
					}
					return (String) value;
				}
		);
	}

	private JdbcEnvironmentImpl getJdbcEnvironmentUsingJdbcMetadata(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry,
			DialectFactory dialectFactory, String explicitDatabaseName,
			Integer explicitDatabaseMajorVersion,
			Integer explicitDatabaseMinorVersion,
			String explicitDatabaseVersion) {
		final JdbcConnectionAccess jdbcConnectionAccess = buildJdbcConnectionAccess( registry );
		final JdbcServicesImpl jdbcServices = new JdbcServicesImpl( registry );
		final TemporaryJdbcSessionOwner temporaryJdbcSessionOwner = new TemporaryJdbcSessionOwner(
				jdbcConnectionAccess,
				jdbcServices,
				new SqlExceptionHelper( false ),
				registry
		);
		temporaryJdbcSessionOwner.transactionCoordinator = registry.requireService( TransactionCoordinatorBuilder.class )
				.buildTransactionCoordinator(
						new JdbcCoordinatorImpl( null, temporaryJdbcSessionOwner, jdbcServices ),
						() -> false
				);

		try {
			return temporaryJdbcSessionOwner.transactionCoordinator.createIsolationDelegate().delegateWork(
					new AbstractReturningWork<>() {
						@Override
						public JdbcEnvironmentImpl execute(Connection connection) {
							try {
								final DatabaseMetaData metadata = connection.getMetaData();
								logDatabaseAndDriver( metadata );

								final DialectResolutionInfo dialectResolutionInfo = new DialectResolutionInfoImpl(
										metadata,
										explicitDatabaseName == null
												? metadata.getDatabaseProductName()
												: explicitDatabaseName,
										explicitDatabaseVersion == null
												? metadata.getDatabaseProductVersion()
												: explicitDatabaseVersion,
										explicitDatabaseMajorVersion == null
												? metadata.getDatabaseMajorVersion()
												: explicitDatabaseMajorVersion,
										explicitDatabaseMinorVersion == null
												? metadata.getDatabaseMinorVersion()
												: explicitDatabaseMinorVersion,
										explicitDatabaseMinorVersion == null
												? databaseMicroVersion( metadata )
												: 0,
										metadata.getDriverName(),
										metadata.getDriverMajorVersion(),
										metadata.getDriverMinorVersion(),
										metadata.getSQLKeywords(),
										configurationValues
								);
								return new JdbcEnvironmentImpl(
										registry,
										dialectFactory.buildDialect( configurationValues, () -> dialectResolutionInfo ),
										metadata,
										jdbcConnectionAccess
								);
							}
							catch (SQLException e) {
								log.unableToObtainConnectionMetadata( e );
							}

							// accessing the JDBC metadata failed
							return getJdbcEnvironmentWithDefaults( configurationValues, registry, dialectFactory );
						}

						private int databaseMicroVersion(DatabaseMetaData metadata) throws SQLException {
							final String version = metadata.getDatabaseProductVersion();
							final String prefix =
									metadata.getDatabaseMajorVersion() + "." + metadata.getDatabaseMinorVersion() + ".";
							if ( version.startsWith(prefix) ) {
								try {
									final String substring = version.substring( prefix.length() );
									final String micro = new StringTokenizer(substring," .,-:;/()[]").nextToken();
									return Integer.parseInt(micro);
								}
								catch (NumberFormatException nfe) {
									return 0;
								}
							}
							else {
								return 0;
							}
						}
					},
					false
			);
		}
		catch ( Exception e ) {
			log.unableToObtainConnectionToQueryMetadata( e );
		}
		// accessing the JDBC metadata failed
		return getJdbcEnvironmentWithDefaults( configurationValues, registry, dialectFactory );
	}

	private static void logDatabaseAndDriver(DatabaseMetaData dbmd) throws SQLException {
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Database ->\n"
							+ "	   name : %s\n"
							+ "	version : %s\n"
							+ "	  major : %s\n"
							+ "	  minor : %s",
					dbmd.getDatabaseProductName(),
					dbmd.getDatabaseProductVersion(),
					dbmd.getDatabaseMajorVersion(),
					dbmd.getDatabaseMinorVersion()
			);
			log.debugf(
					"Driver ->\n"
							+ "	   name : %s\n"
							+ "	version : %s\n"
							+ "	  major : %s\n"
							+ "	  minor : %s",
					dbmd.getDriverName(),
					dbmd.getDriverVersion(),
					dbmd.getDriverMajorVersion(),
					dbmd.getDriverMinorVersion()
			);
			log.debugf( "JDBC version : %s.%s", dbmd.getJDBCMajorVersion(), dbmd.getJDBCMinorVersion() );
		}
	}

	private static boolean explicitDialectConfiguration(String explicitDatabaseName,
			Map<String, Object> configurationValues) {
		return isNotEmpty( explicitDatabaseName ) || isNotNullAndNotEmpty( configurationValues.get( DIALECT ) );
	}

	private static boolean isNotNullAndNotEmpty(Object o) {
		return o != null && ( !(o instanceof String) || !((String) o).isEmpty() );
	}

	private JdbcConnectionAccess buildJdbcConnectionAccess(ServiceRegistryImplementor registry) {
		if ( !isMultiTenancyEnabled( registry ) ) {
			return new ConnectionProviderJdbcConnectionAccess( registry.requireService( ConnectionProvider.class ) );
		}
		else {
			final MultiTenantConnectionProvider<?> multiTenantConnectionProvider =
					registry.getService( MultiTenantConnectionProvider.class );
			return new MultiTenantConnectionProviderJdbcConnectionAccess( multiTenantConnectionProvider );
		}
	}

	public static JdbcConnectionAccess buildBootstrapJdbcConnectionAccess(ServiceRegistryImplementor registry) {
		if ( !isMultiTenancyEnabled( registry ) ) {
			return new ConnectionProviderJdbcConnectionAccess( registry.requireService( ConnectionProvider.class ) );
		}
		else {
			final MultiTenantConnectionProvider<?> multiTenantConnectionProvider =
					registry.getService( MultiTenantConnectionProvider.class );
			return new MultiTenantConnectionProviderJdbcConnectionAccess( multiTenantConnectionProvider );
		}
	}

	public static class ConnectionProviderJdbcConnectionAccess implements JdbcConnectionAccess {
		private final ConnectionProvider connectionProvider;

		public ConnectionProviderJdbcConnectionAccess(ConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		public ConnectionProvider getConnectionProvider() {
			return connectionProvider;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			return connectionProvider.getConnection();
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			connectionProvider.closeConnection( connection );
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}

	public static class MultiTenantConnectionProviderJdbcConnectionAccess implements JdbcConnectionAccess {
		private final MultiTenantConnectionProvider<?> connectionProvider;

		public MultiTenantConnectionProviderJdbcConnectionAccess(MultiTenantConnectionProvider<?> connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		public MultiTenantConnectionProvider<?> getConnectionProvider() {
			return connectionProvider;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			return connectionProvider.getAnyConnection();
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			connectionProvider.releaseAnyConnection( connection );
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}

	private static class DialectResolutionInfoImpl implements DialectResolutionInfo {
		private final DatabaseMetaData databaseMetadata;
		private final String databaseName;
		private final String databaseVersion;
		private final int databaseMajorVersion;
		private final int databaseMinorVersion;
		private final int databaseMicroVersion;
		private final String driverName;
		private final int driverMajorVersion;
		private final int driverMinorVersion;
		private final String sqlKeywords;
		private final Map<String, Object> configurationValues;

		public DialectResolutionInfoImpl(
				DatabaseMetaData databaseMetadata,
				String databaseName,
				String databaseVersion,
				int databaseMajorVersion,
				int databaseMinorVersion,
				int databaseMicroVersion,
				String driverName,
				int driverMajorVersion,
				int driverMinorVersion,
				String sqlKeywords,
				Map<String, Object> configurationValues) {
			this.databaseMetadata = databaseMetadata;
			this.databaseName = databaseName;
			this.databaseVersion = databaseVersion;
			this.databaseMajorVersion = databaseMajorVersion;
			this.databaseMinorVersion = databaseMinorVersion;
			this.databaseMicroVersion = databaseMicroVersion;
			this.driverName = driverName;
			this.driverMajorVersion = driverMajorVersion;
			this.driverMinorVersion = driverMinorVersion;
			this.sqlKeywords = sqlKeywords;
			this.configurationValues = configurationValues;
		}

		public String getSQLKeywords() {
			return sqlKeywords;
		}

		@Override
		public String getDatabaseName() {
			return databaseName;
		}

		@Override
		public String getDatabaseVersion() {
			return databaseVersion;
		}

		@Override
		public int getDatabaseMajorVersion() {
			return databaseMajorVersion;
		}

		@Override
		public int getDatabaseMinorVersion() {
			return databaseMinorVersion;
		}

		@Override
		public int getDatabaseMicroVersion() {
			return databaseMicroVersion;
		}

		@Override
		public String getDriverName() {
			return driverName;
		}

		@Override
		public int getDriverMajorVersion() {
			return driverMajorVersion;
		}

		@Override
		public int getDriverMinorVersion() {
			return driverMinorVersion;
		}

		@Override
		public DatabaseMetaData getDatabaseMetadata() {
			return databaseMetadata;
		}

		@Override
		public String toString() {
			return getMajor() + "." + getMinor();
		}

		@Override
		public Map<String, Object> getConfigurationValues() {
			return configurationValues;
		}
	}

	/**
	 * This is a temporary JdbcSessionOwner for the purpose of passing a connection to the Dialect for initialization.
	 */
	private static class TemporaryJdbcSessionOwner implements JdbcSessionOwner, JdbcSessionContext {

		private final JdbcConnectionAccess jdbcConnectionAccess;
		private final JdbcServices jdbcServices;
		private final ServiceRegistryImplementor serviceRegistry;
		private final boolean jtaTrackByThread;
		private final boolean preferUserTransaction;
		private final boolean connectionProviderDisablesAutoCommit;
		private final PhysicalConnectionHandlingMode connectionHandlingMode;
		private final JpaCompliance jpaCompliance;
		private final SqlExceptionHelper sqlExceptionHelper;
		private static final EmptyJdbcObserver EMPTY_JDBC_OBSERVER = EmptyJdbcObserver.INSTANCE;
		TransactionCoordinator transactionCoordinator;
		private final EmptyEventManager eventManager;

		public TemporaryJdbcSessionOwner(
				JdbcConnectionAccess jdbcConnectionAccess,
				JdbcServices jdbcServices,
				SqlExceptionHelper sqlExceptionHelper,
				ServiceRegistryImplementor serviceRegistry) {
			this.jdbcConnectionAccess = jdbcConnectionAccess;
			this.jdbcServices = jdbcServices;
			this.sqlExceptionHelper = sqlExceptionHelper;
			this.serviceRegistry = serviceRegistry;
			final ConfigurationService configuration = serviceRegistry.requireService( ConfigurationService.class );
			this.jtaTrackByThread = configuration.getSetting( JTA_TRACK_BY_THREAD, BOOLEAN, true );
			this.preferUserTransaction = getBoolean( PREFER_USER_TRANSACTION, configuration.getSettings() );
			this.connectionProviderDisablesAutoCommit =
					getBoolean( CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, configuration.getSettings() );

			final PhysicalConnectionHandlingMode specifiedHandlingMode =
					PhysicalConnectionHandlingMode.interpret( configuration.getSettings().get( CONNECTION_HANDLING ) );
			this.connectionHandlingMode = specifiedHandlingMode != null
					? specifiedHandlingMode
					: serviceRegistry.requireService(TransactionCoordinatorBuilder.class)
							.getDefaultConnectionHandlingMode();

			this.jpaCompliance = new MutableJpaComplianceImpl( Collections.emptyMap(), false );
			this.eventManager = new EmptyEventManager();
		}

		@Override
		public JdbcSessionContext getJdbcSessionContext() {
			return this;
		}

		@Override
		public JdbcConnectionAccess getJdbcConnectionAccess() {
			return jdbcConnectionAccess;
		}

		@Override
		public TransactionCoordinator getTransactionCoordinator() {
			return transactionCoordinator;
		}

		@Override
		public void startTransactionBoundary() {

		}

		@Override
		public void afterTransactionBegin() {

		}

		@Override
		public void beforeTransactionCompletion() {

		}

		@Override
		public void afterTransactionCompletion(boolean successful, boolean delayed) {

		}

		@Override
		public void flushBeforeTransactionCompletion() {

		}

		@Override
		public Integer getJdbcBatchSize() {
			return null;
		}

		@Override
		public EventManager getEventManager() {
			return eventManager;
		}

		@Override
		public boolean isScrollableResultSetsEnabled() {
			return false;
		}

		@Override
		public boolean isGetGeneratedKeysEnabled() {
			return false;
		}

		@Override
		public Integer getFetchSizeOrNull() {
			return null;
		}

		@Override @Deprecated
		public int getFetchSize() {
			return 0;
		}

		@Override
		public boolean doesConnectionProviderDisableAutoCommit() {
			return connectionProviderDisablesAutoCommit;
		}

		@Override
		public boolean isPreferUserTransaction() {
			return preferUserTransaction;
		}

		@Override
		public boolean isJtaTrackByThread() {
			return jtaTrackByThread;
		}

		@Override
		public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
			return connectionHandlingMode;
		}

		@Override
		public StatementInspector getStatementInspector() {
			return null;
		}

		@Override
		public JpaCompliance getJpaCompliance() {
			return jpaCompliance;
		}

		@Override
		public StatisticsImplementor getStatistics() {
			return null;
		}

		@Override @Deprecated
		public JdbcObserver getObserver() {
			return EMPTY_JDBC_OBSERVER;
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return null;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}

		@Override
		public JdbcServices getJdbcServices() {
			return jdbcServices;
		}

		@Override
		public BatchBuilder getBatchBuilder() {
			return null;
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public SqlExceptionHelper getSqlExceptionHelper() {
			return sqlExceptionHelper;
		}

		private static class EmptyJdbcObserver implements JdbcObserver{

			public static final EmptyJdbcObserver INSTANCE = new EmptyJdbcObserver();

			@Override
			public void jdbcConnectionAcquisitionStart() {

			}

			@Override
			public void jdbcConnectionAcquisitionEnd(Connection connection) {

			}

			@Override
			public void jdbcConnectionReleaseStart() {

			}

			@Override
			public void jdbcConnectionReleaseEnd() {

			}

			@Override
			public void jdbcPrepareStatementStart() {

			}

			@Override
			public void jdbcPrepareStatementEnd() {

			}

			@Override
			public void jdbcExecuteStatementStart() {

			}

			@Override
			public void jdbcExecuteStatementEnd() {

			}

			@Override
			public void jdbcExecuteBatchStart() {

			}

			@Override
			public void jdbcExecuteBatchEnd() {

			}
		}
	}
}
