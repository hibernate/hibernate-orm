/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.contribution.jts.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import org.h2gis.utilities.SFSUtilities;

/**
 * @author Steve Ebersole
 */
public class CustomConnectionProvider implements ConnectionProvider, Stoppable {
	private final ConnectionProvider delegate;

	public CustomConnectionProvider(Map configurationValues, ServiceRegistryImplementor registry) {
		delegate = ConnectionProviderInitiator.INSTANCE.initiateService( configurationValues, registry );
		( (Configurable) delegate ).configure( configurationValues );
		( (ServiceRegistryAwareService) delegate ).injectServices( registry );
	}

	@Override
	public Connection getConnection() throws SQLException {
		final Connection wrappedConnection = SFSUtilities.wrapConnection( delegate.getConnection() );
		try {
			final Statement statement = wrappedConnection.createStatement();
			statement.execute( "CREATE ALIAS IF NOT EXISTS H2GIS_SPATIAL FOR \"org.h2gis.functions.factory.H2GISFunctions.load\"" );
			statement.execute( "CALL H2GIS_SPATIAL()" );
			return wrappedConnection;
		}
		catch (SQLException e) {
			throw new JDBCConnectionException( "Error creating H2GIS wrapped Connection", e );
		}
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		delegate.closeConnection( conn );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		return null;
	}

	@Override
	public void stop() {
		if ( delegate instanceof Stoppable ) {
			( (Stoppable) delegate ).stop();
		}
	}
}
