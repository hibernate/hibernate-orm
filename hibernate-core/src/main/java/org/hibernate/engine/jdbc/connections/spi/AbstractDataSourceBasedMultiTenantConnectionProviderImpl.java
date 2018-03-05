/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.spi;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.hibernate.service.UnknownUnwrapTypeException;

/**
 * Basic support for implementations of {@link MultiTenantConnectionProvider} based on DataSources.
 * @author Steve Ebersole
 */
public abstract class AbstractDataSourceBasedMultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider {
	protected abstract DataSource selectAnyDataSource();
	protected abstract DataSource selectDataSource(String tenantIdentifier);

	@Override
	public Connection getAnyConnection() throws SQLException {
		return selectAnyDataSource().getConnection();
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public Connection getConnection(String tenantIdentifier) throws SQLException {
		return selectDataSource( tenantIdentifier ).getConnection();
	}

	@Override
	public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return
			DataSource.class.isAssignableFrom( unwrapType ) ||
			MultiTenantConnectionProvider.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( MultiTenantConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else if ( DataSource.class.isAssignableFrom( unwrapType ) ) {
			return (T) selectAnyDataSource();
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}
}
