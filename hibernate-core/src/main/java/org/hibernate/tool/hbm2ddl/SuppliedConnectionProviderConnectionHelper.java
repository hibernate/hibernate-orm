/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.tool.hbm2ddl;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;

/**
 * A {@link ConnectionHelper} implementation based on a provided
 * {@link ConnectionProvider}.  Essentially, ensures that the connection
 * gets cleaned up, but that the provider itself remains usable since it
 * was externally provided to us.
 *
 * @author Steve Ebersole
 */
class SuppliedConnectionProviderConnectionHelper implements ConnectionHelper {
	private ConnectionProvider provider;
	private Connection connection;
	private boolean toggleAutoCommit;

	public SuppliedConnectionProviderConnectionHelper(ConnectionProvider provider) {
		this.provider = provider;
	}

	public void prepare(boolean needsAutoCommit) throws SQLException {
		connection = provider.getConnection();
		toggleAutoCommit = needsAutoCommit && !connection.getAutoCommit();
		if ( toggleAutoCommit ) {
			try {
				connection.commit();
			}
			catch( Throwable ignore ) {
				// might happen with a managed connection
			}
			connection.setAutoCommit( true );
		}
	}

	public Connection getConnection() throws SQLException {
		return connection;
	}

	public void release() throws SQLException {
		// we only release the connection
		if ( connection != null ) {
			new SqlExceptionHelper().logAndClearWarnings( connection );
			if ( toggleAutoCommit ) {
				connection.setAutoCommit( false );
			}
			provider.closeConnection( connection );
			connection = null;
		}
	}
}
