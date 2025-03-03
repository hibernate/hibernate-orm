/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides callback access into the context in which the LOB is to be created.
 *
 * @author Steve Ebersole
 */
public interface LobCreationContext {
	/**
	 * The callback contract for making use of the JDBC {@link Connection}.
	 */
	@FunctionalInterface
	interface Callback<T> {
		/**
		 * Perform whatever actions are necessary using the provided JDBC {@link Connection}.
		 *
		 * @param connection The JDBC {@link Connection}.
		 *
		 * @return The created LOB.
		 *
		 * @throws SQLException Indicates trouble accessing the JDBC driver to create the LOB
		 */
		T executeOnConnection(Connection connection) throws SQLException;

		default T from(Connection connection) throws SQLException {
			return executeOnConnection( connection );
		}
	}

	/**
	 * Execute the given callback, making sure it has access to a viable JDBC {@link Connection}.
	 *
	 * @param callback The callback to execute .
	 * @param <T> The Java type of the type of LOB being created.  One of {@link java.sql.Blob},
	 * {@link java.sql.Clob}, {@link java.sql.NClob}
	 *
	 * @return The LOB created by the callback.
	 */
	<T> T execute(Callback<T> callback);

	default <T> T fromContext(Callback<T> callback) {
		return execute( callback );
	}
}
