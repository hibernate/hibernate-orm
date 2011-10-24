/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.jdbc.connections.internal;
import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;

/**
 * An implementation of the {@link ConnectionProvider} interface that simply throws an exception when a connection
 * is requested, the assumption being that the application is responsible for handing the connection to use to
 * the session
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class UserSuppliedConnectionProviderImpl implements ConnectionProvider {
	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
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

	/**
	 * {@inheritDoc}
	 */
	public Connection getConnection() throws SQLException {
		throw new UnsupportedOperationException( "The application must supply JDBC connections" );
	}

	/**
	 * {@inheritDoc}
	 */
	public void closeConnection(Connection conn) throws SQLException {
		throw new UnsupportedOperationException( "The application must supply JDBC connections" );
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean supportsAggressiveRelease() {
		return false;
	}
}
