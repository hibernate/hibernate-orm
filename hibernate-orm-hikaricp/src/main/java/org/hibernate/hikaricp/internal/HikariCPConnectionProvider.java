/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.hikaricp.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import org.jboss.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * HikariCP Connection provider for Hibernate.
 *
 * @author Brett Wooldridge
 * @author Luca Burgazzoli
 */
public class HikariCPConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

	private static final long serialVersionUID = -9131625057941275711L;

	private static final Logger LOGGER = Logger.getLogger( HikariCPConnectionProvider.class );

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

	@SuppressWarnings("rawtypes")
	@Override
	public void configure(Map props) throws HibernateException {
		try {
			LOGGER.debug( "Configuring HikariCP" );

			hcfg = HikariConfigurationUtil.loadConfiguration( props );
			hds = new HikariDataSource( hcfg );

		}
		catch (Exception e) {
			throw new HibernateException( e );
		}

		LOGGER.debug( "HikariCP Configured" );
	}

	// *************************************************************************
	// ConnectionProvider
	// *************************************************************************

	@Override
	public Connection getConnection() throws SQLException {
		Connection conn = null;
		if ( hds != null ) {
			conn = hds.getConnection();
		}

		return conn;
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		conn.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean isUnwrappableAs(Class unwrapType) {
		return ConnectionProvider.class.equals( unwrapType )
				|| HikariCPConnectionProvider.class.isAssignableFrom( unwrapType )
				|| DataSource.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType ) ||
				HikariCPConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
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
		hds.shutdown();
	}
}
