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

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

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
	public JdbcEnvironment initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final DialectFactory dialectFactory = registry.getService( DialectFactory.class );

		// 'hibernate.temp.use_jdbc_metadata_defaults' is a temporary magic value.
		// The need for it is intended to be alleviated with future development, thus it is
		// not defined as an Environment constant...
		//
		// it is used to control whether we should consult the JDBC metadata to determine
		// certain Settings default values; it is useful to *not* do this when the database
		// may not be available (mainly in tools usage).
		boolean useJdbcMetadata = ConfigurationHelper.getBoolean(
				"hibernate.temp.use_jdbc_metadata_defaults",
				configurationValues,
				true
		);

		if ( useJdbcMetadata ) {
			final JdbcConnectionAccess jdbcConnectionAccess = buildJdbcConnectionAccess( configurationValues, registry );
			try {
				final Connection connection = jdbcConnectionAccess.obtainConnection();
				try {
					final DatabaseMetaData dbmd = connection.getMetaData();
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

					Dialect dialect = dialectFactory.buildDialect(
							configurationValues,
							new DialectResolutionInfoSource() {
								@Override
								public DialectResolutionInfo getDialectResolutionInfo() {
									try {
										return new DatabaseMetaDataDialectResolutionInfoAdapter( connection.getMetaData() );
									}
									catch ( SQLException sqlException ) {
										throw new HibernateException(
												"Unable to access java.sql.DatabaseMetaData to determine appropriate Dialect to use",
												sqlException
										);
									}
								}
							}
					);
					return new JdbcEnvironmentImpl(
							registry,
							dialect,
							dbmd
					);
				}
				catch (SQLException e) {
					log.unableToObtainConnectionMetadata( e.getMessage() );
				}
				finally {
					try {
						jdbcConnectionAccess.releaseConnection( connection );
					}
					catch (SQLException ignore) {
					}
				}
			}
			catch (Exception e) {
				log.unableToObtainConnectionToQueryMetadata( e.getMessage() );
			}
		}

		// if we get here, either we were asked to not use JDBC metadata or accessing the JDBC metadata failed.
		return new JdbcEnvironmentImpl( registry, dialectFactory.buildDialect( configurationValues, null ) );
	}

	private JdbcConnectionAccess buildJdbcConnectionAccess(Map configValues, ServiceRegistryImplementor registry) {
		final MultiTenancyStrategy multiTenancyStrategy = MultiTenancyStrategy.determineMultiTenancyStrategy(
				configValues
		);
		if ( !multiTenancyStrategy.requiresMultiTenantConnectionProvider() ) {
			ConnectionProvider connectionProvider = registry.getService( ConnectionProvider.class );
			return new ConnectionProviderJdbcConnectionAccess( connectionProvider );
		}
		else {
			final MultiTenantConnectionProvider multiTenantConnectionProvider = registry.getService( MultiTenantConnectionProvider.class );
			return new MultiTenantConnectionProviderJdbcConnectionAccess( multiTenantConnectionProvider );
		}
	}

	public static JdbcConnectionAccess buildBootstrapJdbcConnectionAccess(
			MultiTenancyStrategy multiTenancyStrategy,
			ServiceRegistryImplementor registry) {
		if ( !multiTenancyStrategy.requiresMultiTenantConnectionProvider() ) {
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
}
