/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;

/**
 * An implementation of the {@link ConnectionProvider} interface that simply throws an
 * exception when a connection is requested, the assumption being that the application
 * is responsible for handing the connection to use to the session.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class UserSuppliedConnectionProviderImpl implements ConnectionProvider {
	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return ConnectionProvider.class.equals( unwrapType ) ||
				UserSuppliedConnectionProviderImpl.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType ) ||
				UserSuppliedConnectionProviderImpl.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		throw new UnsupportedOperationException( "The application must supply JDBC connections" );
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		throw new UnsupportedOperationException( "The application must supply JDBC connections" );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}
}
