/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.spi;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.service.UnknownUnwrapTypeException;

/**
 * Basic support for {@link MultiTenantConnectionProvider} implementations using
 * individual {@link ConnectionProvider} instances per tenant behind the scenes.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractMultiTenantConnectionProvider implements MultiTenantConnectionProvider {
	protected abstract ConnectionProvider getAnyConnectionProvider();
	protected abstract ConnectionProvider selectConnectionProvider(String tenantIdentifier);

	@Override
	public Connection getAnyConnection() throws SQLException {
		return getAnyConnectionProvider().getConnection();
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		getAnyConnectionProvider().closeConnection( connection );
	}

	@Override
	public Connection getConnection(String tenantIdentifier) throws SQLException {
		return selectConnectionProvider( tenantIdentifier ).getConnection();
	}

	@Override
	public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
		selectConnectionProvider( tenantIdentifier ).closeConnection( connection );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return getAnyConnectionProvider().supportsAggressiveRelease();
	}

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return ConnectionProvider.class.equals( unwrapType ) ||
				MultiTenantConnectionProvider.class.equals( unwrapType ) ||
				AbstractMultiTenantConnectionProvider.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( isUnwrappableAs( unwrapType ) ) {
			return (T) this;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}
}
