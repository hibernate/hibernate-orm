/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Template (as in template pattern) support for ConnectionCreator implementors.
 *
 * @author Steve Ebersole
 */
public abstract class BasicConnectionCreator implements ConnectionCreator {
	private final ServiceRegistryImplementor serviceRegistry;

	private final String url;
	private final Properties connectionProps;

	private final boolean autoCommit;
	private final Integer isolation;

	public BasicConnectionCreator(
			ServiceRegistryImplementor serviceRegistry,
			String url,
			Properties connectionProps,
			boolean autocommit,
			Integer isolation) {
		this.serviceRegistry = serviceRegistry;
		this.url = url;
		this.connectionProps = connectionProps;
		this.autoCommit = autocommit;
		this.isolation = isolation;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public Connection createConnection() {
		final Connection conn = makeConnection( url, connectionProps );

		try {
			if ( isolation != null ) {
				conn.setTransactionIsolation( isolation );
			}
		}
		catch (SQLException e) {
			throw convertSqlException( "Unable to set transaction isolation (" + isolation + ")", e );
		}

		try {
			if ( conn.getAutoCommit() != autoCommit ) {
				conn.setAutoCommit( autoCommit );
			}
		}
		catch (SQLException e) {
			throw convertSqlException( "Unable to set auto-commit (" + autoCommit + ")", e );
		}

		return conn;
	}

	protected JDBCException convertSqlException(String message, SQLException e) {
		return serviceRegistry.getService( JdbcServices.class ).getSqlExceptionHelper().convert( e, message, null );
	}

	protected abstract Connection makeConnection(String url, Properties connectionProps);
}
