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
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.DIALECT_DB_MAJOR_VERSION;
import static org.hibernate.cfg.AvailableSettings.DIALECT_DB_MINOR_VERSION;
import static org.hibernate.cfg.AvailableSettings.DIALECT_DB_NAME;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DB_MAJOR_VERSION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DB_MINOR_VERSION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DB_NAME;
import static org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl.isMultiTenancyEnabled;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
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

	@Override
	public Class<JdbcEnvironment> getServiceInitiated() {
		return JdbcEnvironment.class;
	}

	@Override
	public JdbcEnvironment initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final DialectFactory dialectFactory = registry.getService( DialectFactory.class );

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

		if ( useJdbcMetadata( configurationValues ) ) {
			return getJdbcEnvironmentUsingJdbcMetadata(
					configurationValues,
					registry,
					dialectFactory,
					explicitDatabaseName,
					explicitDatabaseMajorVersion,
					explicitDatabaseMinorVersion,
					explicitDatabaseVersion);
		}
		else if ( explicitDialectConfiguration(
				configurationValues,
				explicitDatabaseName,
				explicitDatabaseMajorVersion,
				explicitDatabaseMinorVersion,
				explicitDatabaseVersion) ) {
			return getJdbcEnvironmentWithExplicitConfiguration(
					configurationValues,
					registry,
					dialectFactory,
					explicitDatabaseName,
					explicitDatabaseMajorVersion,
					explicitDatabaseMinorVersion,
					explicitDatabaseVersion
			);
		}
		else {
			return getJdbcEnvironmentWithDefaults( configurationValues, registry, dialectFactory );
		}
	}

	private static JdbcEnvironmentImpl getJdbcEnvironmentWithDefaults(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry,
			DialectFactory dialectFactory) {
		return new JdbcEnvironmentImpl(
				registry,
				dialectFactory.buildDialect( configurationValues, null )
		);
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
				explicitDatabaseVersion != null ? explicitDatabaseVersion : "0",
				explicitDatabaseMajorVersion != null ? explicitDatabaseMajorVersion : 0,
				explicitDatabaseMinorVersion != null ? explicitDatabaseMinorVersion : 0,
				null,
				0,
				0,
				null
		);
		return new JdbcEnvironmentImpl(
				registry,
				dialectFactory.buildDialect( configurationValues, () -> dialectResolutionInfo )
		);
	}

	// 'hibernate.temp.use_jdbc_metadata_defaults' is a temporary magic value.
	// The need for it is intended to be alleviated with future development, thus it is
	// not defined as an Environment constant...
	//
	// it is used to control whether we should consult the JDBC metadata to determine
	// certain default values; it is useful to *not* do this when the database
	// may not be available (mainly in tools usage).
	private static boolean useJdbcMetadata(Map<String, Object> configurationValues) {
		return getBoolean(
				"hibernate.temp.use_jdbc_metadata_defaults",
				configurationValues,
				true
		);
	}

	private static String getExplicitDatabaseVersion(
			Map<String, Object> configurationValues,
			Integer configuredDatabaseMajorVersion,
			Integer configuredDatabaseMinorVersion) {
		return coalesceSuppliedValues(
				() -> (String) configurationValues.get(AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION),
				() -> {
					final Object value = configurationValues.get(AvailableSettings.DIALECT_DB_VERSION);
					if ( value != null ) {
						DEPRECATION_LOGGER.deprecatedSetting(
								AvailableSettings.DIALECT_DB_VERSION,
								AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION
						);
					}
					return (String) value;
				}
				,
				() -> {
					if ( configuredDatabaseMajorVersion != null ) {
						return configuredDatabaseMinorVersion == null
								? configuredDatabaseMajorVersion.toString()
								: (configuredDatabaseMajorVersion + "." + configuredDatabaseMinorVersion);
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
		final JdbcConnectionAccess jdbcConnectionAccess = buildJdbcConnectionAccess(registry);
		try {
			final Connection connection = jdbcConnectionAccess.obtainConnection();
			try {
				final DatabaseMetaData dbmd = connection.getMetaData();
				logDatabaseAndDriver( dbmd );

				final String databaseName;
				final String databaseVersion;
				final int databaseMajorVersion;
				final int databaseMinorVersion;

				if ( explicitDatabaseName == null ) {
					databaseName = dbmd.getDatabaseProductName();
				}
				else {
					databaseName = explicitDatabaseName;
				}
				if ( explicitDatabaseVersion == null ) {
					databaseVersion = dbmd.getDatabaseProductVersion();
				}
				else {
					databaseVersion = explicitDatabaseVersion;
				}
				if ( explicitDatabaseMajorVersion == null ) {
					databaseMajorVersion = dbmd.getDatabaseMajorVersion();
				}
				else {
					databaseMajorVersion = explicitDatabaseMajorVersion;
				}
				if ( explicitDatabaseMinorVersion == null ) {
					databaseMinorVersion = dbmd.getDatabaseMinorVersion();
				}
				else {
					databaseMinorVersion = explicitDatabaseMinorVersion;
				}

				final DialectResolutionInfo dialectResolutionInfo = new DialectResolutionInfoImpl(
						dbmd,
						databaseName,
						databaseVersion,
						databaseMajorVersion,
						databaseMinorVersion,
						dbmd.getDriverName(),
						dbmd.getDriverMajorVersion(),
						dbmd.getDriverMinorVersion(),
						dbmd.getSQLKeywords()
				);
				return new JdbcEnvironmentImpl(
						registry,
						dialectFactory.buildDialect( configurationValues, () -> dialectResolutionInfo ),
						dbmd,
						jdbcConnectionAccess
				);
			}
			catch ( SQLException e ) {
				log.unableToObtainConnectionMetadata( e );
			}
			finally {
				try {
					jdbcConnectionAccess.releaseConnection( connection );
				}
				catch ( SQLException ignore ) {
				}
			}
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
							+ "       name : %s\n"
							+ "    version : %s\n"
							+ "      major : %s\n"
							+ "      minor : %s",
					dbmd.getDatabaseProductName(),
					dbmd.getDatabaseProductVersion(),
					dbmd.getDatabaseMajorVersion(),
					dbmd.getDatabaseMinorVersion()
			);
			log.debugf(
					"Driver ->\n"
							+ "       name : %s\n"
							+ "    version : %s\n"
							+ "      major : %s\n"
							+ "      minor : %s",
					dbmd.getDriverName(),
					dbmd.getDriverVersion(),
					dbmd.getDriverMajorVersion(),
					dbmd.getDriverMinorVersion()
			);
			log.debugf( "JDBC version : %s.%s", dbmd.getJDBCMajorVersion(), dbmd.getJDBCMinorVersion() );
		}
	}

	private static boolean explicitDialectConfiguration(
			Map<String, Object> configurationValues,
			String explicitDatabaseName,
			Integer explicitDatabaseMajorVersion,
			Integer explicitDatabaseMinorVersion,
			String explicitDatabaseVersion) {
		return ( isNotEmpty(explicitDatabaseVersion) || explicitDatabaseMajorVersion != null || explicitDatabaseMinorVersion != null )
			&& ( isNotEmpty(explicitDatabaseName) || isNotNullAndNotEmpty( configurationValues.get(AvailableSettings.DIALECT) ) );
	}

	private static boolean isNotNullAndNotEmpty(Object o) {
		return o != null && ( !(o instanceof String) || !((String) o).isEmpty() );
	}

	private JdbcConnectionAccess buildJdbcConnectionAccess(ServiceRegistryImplementor registry) {
		if ( !isMultiTenancyEnabled( registry ) ) {
			ConnectionProvider connectionProvider = registry.getService( ConnectionProvider.class );
			return new ConnectionProviderJdbcConnectionAccess( connectionProvider );
		}
		else {
			final MultiTenantConnectionProvider multiTenantConnectionProvider = registry.getService( MultiTenantConnectionProvider.class );
			return new MultiTenantConnectionProviderJdbcConnectionAccess( multiTenantConnectionProvider );
		}
	}

	public static JdbcConnectionAccess buildBootstrapJdbcConnectionAccess(ServiceRegistryImplementor registry) {
		if ( !isMultiTenancyEnabled( registry ) ) {
			ConnectionProvider connectionProvider = registry.getService( ConnectionProvider.class );
			return new ConnectionProviderJdbcConnectionAccess( connectionProvider );
		}
		else {
			final MultiTenantConnectionProvider multiTenantConnectionProvider = registry.getService( MultiTenantConnectionProvider.class );
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
		private final MultiTenantConnectionProvider connectionProvider;

		public MultiTenantConnectionProviderJdbcConnectionAccess(MultiTenantConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		public MultiTenantConnectionProvider getConnectionProvider() {
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
		private final String driverName;
		private final int driverMajorVersion;
		private final int driverMinorVersion;
		private final String sqlKeywords;

		public DialectResolutionInfoImpl(
				DatabaseMetaData databaseMetadata,
				String databaseName,
				String databaseVersion,
				int databaseMajorVersion,
				int databaseMinorVersion,
				String driverName,
				int driverMajorVersion,
				int driverMinorVersion,
				String sqlKeywords) {
			this.databaseMetadata = databaseMetadata;
			this.databaseName = databaseName;
			this.databaseVersion = databaseVersion;
			this.databaseMajorVersion = databaseMajorVersion;
			this.databaseMinorVersion = databaseMinorVersion;
			this.driverName = driverName;
			this.driverMajorVersion = driverMajorVersion;
			this.driverMinorVersion = driverMinorVersion;
			this.sqlKeywords = sqlKeywords;
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
	}
}
