/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 */
package org.hibernate.test.cache.jbc.functional.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;

/**
 * A {@link ConnectionProvider} implementation adding JTA-style transactionality
 * around the returned connections using the {@link DualNodeJtaTransactionManagerImpl}.
 *
 * @author Brian Stansberry
 */
public class DualNodeConnectionProviderImpl implements ConnectionProvider {
	private static ConnectionProvider actualConnectionProvider = ConnectionProviderFactory.newConnectionProvider();

	private String nodeId;
	private boolean isTransactional;

	public static ConnectionProvider getActualConnectionProvider() {
		return actualConnectionProvider;
	}

	public void configure(Properties props) throws HibernateException {
        nodeId = props.getProperty(DualNodeTestUtil.NODE_ID_PROP);
        if (nodeId == null)
            throw new HibernateException(DualNodeTestUtil.NODE_ID_PROP + " not configured");
	}

	public Connection getConnection() throws SQLException {
		DualNodeJtaTransactionImpl currentTransaction = DualNodeJtaTransactionManagerImpl.getInstance(nodeId).getCurrentTransaction();
		if ( currentTransaction == null ) {
			isTransactional = false;
			return actualConnectionProvider.getConnection();
		}
		else {
			isTransactional = true;
			Connection connection = currentTransaction.getEnlistedConnection();
			if ( connection == null ) {
				connection = actualConnectionProvider.getConnection();
				currentTransaction.enlistConnection( connection );
			}
			return connection;
		}
	}

	public void closeConnection(Connection conn) throws SQLException {
		if ( !isTransactional ) {
			conn.close();
		}
	}

	public void close() throws HibernateException {
		actualConnectionProvider.close();
	}

	public boolean supportsAggressiveRelease() {
		return true;
	}
}
