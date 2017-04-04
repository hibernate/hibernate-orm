/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public static interface Callback<T> {
		/**
		 * Perform whatever actions are necessary using the provided JDBC {@link Connection}.
		 *
		 * @param connection The JDBC {@link Connection}.
		 *
		 * @return The created LOB.
		 *
		 * @throws SQLException Indicates trouble accessing the JDBC driver to create the LOB
		 */
		public T executeOnConnection(Connection connection) throws SQLException;
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
	public <T> T execute(Callback<T> callback);
}
