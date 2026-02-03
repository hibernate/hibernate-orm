/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.ServiceRegistry;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Steve Ebersole
 */
public class JdbcUtils {
	@FunctionalInterface
	public interface ConnectionAction {
		void execute(Connection connection) throws SQLException;
	}

	public static void withConnection(ServiceRegistry serviceRegistry, ConnectionAction action) {
		final ConnectionProvider connections = serviceRegistry.requireService( ConnectionProvider.class );
		try {
			final var connection = connections.getConnection();
			try {
				action.execute( connection );
			}
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
			finally {
				connections.closeConnection( connection );
			}
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
	}

	public static void withConnection(ConnectionProvider connections, ConnectionAction action) {
		try {
			final var connection = connections.getConnection();
			try {
				action.execute( connection );
			}
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
			finally {
				connections.closeConnection( connection );
			}
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
	}
}
