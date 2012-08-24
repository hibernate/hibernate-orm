/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.service.jdbc.env.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class JdbcEnvironmentInitiator implements BasicServiceInitiator<JdbcEnvironment> {
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

					return new JdbcEnvironmentImpl(
							registry,
							dialectFactory.buildDialect( configurationValues, connection ),
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
		final MultiTenancyStrategy multiTenancyStrategy = MultiTenancyStrategy.determineMultiTenancyStrategy( configValues );
		if ( MultiTenancyStrategy.NONE == multiTenancyStrategy ) {
			ConnectionProvider connectionProvider = registry.getService( ConnectionProvider.class );
			return new ConnectionProviderJdbcConnectionAccess( connectionProvider );
		}
		else {
			final MultiTenantConnectionProvider multiTenantConnectionProvider = registry.getService( MultiTenantConnectionProvider.class );
			return new MultiTenantConnectionProviderJdbcConnectionAccess( multiTenantConnectionProvider );
		}
	}

	private static class ConnectionProviderJdbcConnectionAccess implements JdbcConnectionAccess {
		private final ConnectionProvider connectionProvider;

		public ConnectionProviderJdbcConnectionAccess(ConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
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

	private static class MultiTenantConnectionProviderJdbcConnectionAccess implements JdbcConnectionAccess {
		private final MultiTenantConnectionProvider connectionProvider;

		public MultiTenantConnectionProviderJdbcConnectionAccess(MultiTenantConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
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
