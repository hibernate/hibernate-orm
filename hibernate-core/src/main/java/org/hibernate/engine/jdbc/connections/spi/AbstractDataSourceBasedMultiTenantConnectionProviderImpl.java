/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.spi;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.hibernate.service.UnknownUnwrapTypeException;

/**
 * Basic support for implementations of {@link MultiTenantConnectionProvider} based on {@link DataSource}s.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDataSourceBasedMultiTenantConnectionProviderImpl<T>
		implements MultiTenantConnectionProvider<T> {
	protected abstract DataSource selectAnyDataSource();
	protected abstract DataSource selectDataSource(T tenantIdentifier);

	@Override
	public Connection getAnyConnection() throws SQLException {
		return selectAnyDataSource().getConnection();
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public Connection getConnection(T tenantIdentifier) throws SQLException {
		return selectDataSource( tenantIdentifier ).getConnection();
	}

	@Override
	public void releaseConnection(T tenantIdentifier, Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return unwrapType.isInstance( this )
			|| unwrapType.isAssignableFrom( DataSource.class );
	}

	@Override
	public <X> X unwrap(Class<X> unwrapType) {
		if ( unwrapType.isInstance( this ) ) {
			return unwrapType.cast( this );
		}
		else if ( unwrapType.isAssignableFrom( DataSource.class ) ) {
			return unwrapType.cast( selectAnyDataSource() );
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}
}
