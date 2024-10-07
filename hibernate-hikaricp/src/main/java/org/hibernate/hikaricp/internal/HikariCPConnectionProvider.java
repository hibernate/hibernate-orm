/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.hikaricp.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.allowJdbcMetadataAccess;

/**
 * HikariCP Connection provider for Hibernate.
 *
 * @author Brett Wooldridge
 * @author Luca Burgazzoli
 */
public class HikariCPConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

	private static final long serialVersionUID = -9131625057941275711L;
	private boolean isMetadataAccessAllowed = true;

	/**
	 * HikariCP configuration.
	 */
	private HikariConfig hcfg = null;

	/**
	 * HikariCP data source.
	 */
	private HikariDataSource hds = null;

	// *************************************************************************
	// Configurable
	// *************************************************************************

	@Override
	public void configure(Map<String, Object> props) throws HibernateException {
		try {
			isMetadataAccessAllowed = allowJdbcMetadataAccess( props );

			ConnectionInfoLogger.INSTANCE.configureConnectionPool( "HikariCP" );

			hcfg = HikariConfigurationUtil.loadConfiguration( props );
			hds = new HikariDataSource( hcfg );
		}
		catch (Exception e) {
			ConnectionInfoLogger.INSTANCE.unableToInstantiateConnectionPool( e );
			throw new HibernateException( e );
		}
	}

	// *************************************************************************
	// ConnectionProvider
	// *************************************************************************

	@Override
	public Connection getConnection() throws SQLException {
		return hds != null ? hds.getConnection() : null;
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		return new DatabaseConnectionInfoImpl(
				hcfg.getJdbcUrl(),
				// Attempt to resolve the driver name from the dialect, in case it wasn't explicitly set and access to
				// the database metadata is allowed
				!StringHelper.isBlank( hcfg.getDriverClassName() ) ? hcfg.getDriverClassName() : extractDriverNameFromMetadata(),
				dialect.getVersion(),
				Boolean.toString( hcfg.isAutoCommit() ),
				hcfg.getTransactionIsolation(),
				hcfg.getMinimumIdle(),
				hcfg.getMaximumPoolSize()
		);
	}

	private String extractDriverNameFromMetadata() {
		if (isMetadataAccessAllowed) {
			try ( Connection conn = getConnection() ) {
				DatabaseMetaData dbmd = conn.getMetaData();
				return dbmd.getDriverName();
			}
			catch (SQLException e) {
				// Do nothing
			}
		}
		return null;
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return ConnectionProvider.class.equals( unwrapType )
			|| HikariCPConnectionProvider.class.isAssignableFrom( unwrapType )
			|| DataSource.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType )
				|| HikariCPConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else if ( DataSource.class.isAssignableFrom( unwrapType ) ) {
			return (T) hds;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	// *************************************************************************
	// Stoppable
	// *************************************************************************

	@Override
	public void stop() {
		if ( hds != null ) {
			ConnectionInfoLogger.INSTANCE.cleaningUpConnectionPool( "HikariCP" );
			hds.close();
		}
	}
}
