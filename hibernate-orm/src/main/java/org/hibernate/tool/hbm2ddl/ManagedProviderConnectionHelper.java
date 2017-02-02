/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * A {@link ConnectionHelper} implementation based on an internally
 * built and managed {@link ConnectionProvider}.
 *
 * @author Steve Ebersole
 *
 * @deprecated Everything in this package has been replaced with
 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool} and friends.
 */
@Deprecated
class ManagedProviderConnectionHelper implements ConnectionHelper {
	private Properties cfgProperties;
	private StandardServiceRegistryImpl serviceRegistry;
	private Connection connection;

	public ManagedProviderConnectionHelper(Properties cfgProperties) {
		this.cfgProperties = cfgProperties;
	}

	public void prepare(boolean needsAutoCommit) throws SQLException {
		serviceRegistry = createServiceRegistry( cfgProperties );
		connection = serviceRegistry.getService( ConnectionProvider.class ).getConnection();
		if ( needsAutoCommit && ! connection.getAutoCommit() ) {
			connection.commit();
			connection.setAutoCommit( true );
		}
	}

	private static StandardServiceRegistryImpl createServiceRegistry(Properties properties) {
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		return (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().applySettings( properties ).build();
	}

	public Connection getConnection() throws SQLException {
		return connection;
	}

	public void release() throws SQLException {
		try {
			releaseConnection();
		}
		finally {
			releaseServiceRegistry();
		}
	}

	private void releaseConnection() throws SQLException {
		if ( connection != null ) {
			try {
				serviceRegistry.getService( JdbcEnvironment.class ).getSqlExceptionHelper().logAndClearWarnings(
						connection );
			}
			finally {
				try {
					serviceRegistry.getService( ConnectionProvider.class ).closeConnection( connection );
				}
				finally {
					connection = null;
				}
			}
		}
	}

	private void releaseServiceRegistry() {
		if ( serviceRegistry != null ) {
			try {
				serviceRegistry.destroy();
			}
			finally {
				serviceRegistry = null;
			}
		}
	}
}
