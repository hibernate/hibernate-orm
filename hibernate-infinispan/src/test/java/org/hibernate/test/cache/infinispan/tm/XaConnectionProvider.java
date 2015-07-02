/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.tm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.testing.env.ConnectionProviderBuilder;

/**
 * XaConnectionProvider.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class XaConnectionProvider implements ConnectionProvider {
	private final static ConnectionProvider DEFAULT_CONNECTION_PROVIDER = ConnectionProviderBuilder.buildConnectionProvider();
	private final ConnectionProvider actualConnectionProvider;
	private boolean isTransactional;

	public XaConnectionProvider() {
		this(DEFAULT_CONNECTION_PROVIDER);
	}

	public XaConnectionProvider(ConnectionProvider connectionProvider) {
		this.actualConnectionProvider = connectionProvider;
	}

	public ConnectionProvider getActualConnectionProvider() {
		return actualConnectionProvider;
	}

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return XaConnectionProvider.class.isAssignableFrom( unwrapType ) ||
				ConnectionProvider.class.equals( unwrapType ) ||
				actualConnectionProvider.getClass().isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( XaConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else if ( ConnectionProvider.class.isAssignableFrom( unwrapType ) ||
				actualConnectionProvider.getClass().isAssignableFrom( unwrapType ) ) {
			return (T) getActualConnectionProvider();
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	public void configure(Properties props) throws HibernateException {
	}

	public Connection getConnection() throws SQLException {
		XaTransactionImpl currentTransaction = XaTransactionManagerImpl.getInstance().getCurrentTransaction();
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
		if ( actualConnectionProvider instanceof Stoppable ) {
			((Stoppable) actualConnectionProvider).stop();
		}
	}

	public boolean supportsAggressiveRelease() {
		return true;
	}
}
