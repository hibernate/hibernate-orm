/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.hikaricp.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

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
				|| HikariCPConnectionProvider.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> unwrapType) {
		if ( isUnwrappableAs( unwrapType ) ) {
			return (T) this;
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
