/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.spi;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.service.UnknownUnwrapTypeException;

/**
 * Basic support for {@link MultiTenantConnectionProvider} implementations using
 * an individual {@link ConnectionProvider} instance per tenant behind the scenes.
 * <p>
 * This class is meant to be subclassed to implement application-specific
 * requirements.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractMultiTenantConnectionProvider<T> implements MultiTenantConnectionProvider<T> {
	protected abstract ConnectionProvider getAnyConnectionProvider();
	protected abstract ConnectionProvider selectConnectionProvider(T tenantIdentifier);

	@Override
	public Connection getAnyConnection() throws SQLException {
		return getAnyConnectionProvider().getConnection();
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		getAnyConnectionProvider().closeConnection( connection );
	}

	@Override
	public Connection getConnection(T tenantIdentifier) throws SQLException {
		return selectConnectionProvider( tenantIdentifier ).getConnection();
	}

	@Override
	public void releaseConnection(T tenantIdentifier, Connection connection) throws SQLException {
		selectConnectionProvider( tenantIdentifier ).closeConnection( connection );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return getAnyConnectionProvider().supportsAggressiveRelease();
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return unwrapType.isInstance( this )
			|| unwrapType.isAssignableFrom( ConnectionProvider.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> unwrapType) {
		if ( unwrapType.isInstance( this ) ) {
			return (T) this;
		}
		else if ( unwrapType.isAssignableFrom( ConnectionProvider.class ) ) {
			return (T) getAnyConnectionProvider();
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}
}
