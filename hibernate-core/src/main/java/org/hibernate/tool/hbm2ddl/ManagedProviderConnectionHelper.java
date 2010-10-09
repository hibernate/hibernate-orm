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

import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.util.JDBCExceptionReporter;

import java.util.Properties;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A {@link ConnectionHelper} implementation based on an internally
 * built and managed {@link ConnectionProvider}.
 *
 * @author Steve Ebersole
 */
class ManagedProviderConnectionHelper implements ConnectionHelper {
	private Properties cfgProperties;
	private ConnectionProvider connectionProvider;
	private Connection connection;

	public ManagedProviderConnectionHelper(Properties cfgProperties) {
		this.cfgProperties = cfgProperties;
	}

	public void prepare(boolean needsAutoCommit) throws SQLException {
		connectionProvider = ConnectionProviderFactory.newConnectionProvider( cfgProperties );
		connection = connectionProvider.getConnection();
		if ( needsAutoCommit && !connection.getAutoCommit() ) {
			connection.commit();
			connection.setAutoCommit( true );
		}
	}

	public Connection getConnection() throws SQLException {
		return connection;
	}

	public void release() throws SQLException {
		if ( connection != null ) {
			try {
				JDBCExceptionReporter.logAndClearWarnings( connection );
				connectionProvider.closeConnection( connection );
			}
			finally {
				connectionProvider.close();
			}
		}
		connection = null;
	}
}
