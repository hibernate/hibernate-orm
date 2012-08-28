/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
