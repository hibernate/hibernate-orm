/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.spi;
import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.service.Service;
import org.hibernate.service.spi.Wrapped;

/**
 * A contract for obtaining JDBC connections.
 * <p/>
 * Implementors might also implement connection pooling.
 * <p/>
 * Implementors should provide a public default constructor.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface ConnectionProvider extends Service, Wrapped {
	/**
	 * Obtains a connection for Hibernate use according to the underlying strategy of this provider.
	 *
	 * @return The obtained JDBC connection
	 *
	 * @throws SQLException Indicates a problem opening a connection
	 * @throws org.hibernate.HibernateException Indicates a problem otherwise obtaining a connection.
	 */
	public Connection getConnection() throws SQLException;

	/**
	 * Release a connection from Hibernate use.
	 *
	 * @param conn The JDBC connection to release
	 *
	 * @throws SQLException Indicates a problem closing the connection
	 * @throws org.hibernate.HibernateException Indicates a problem otherwise releasing a connection.
	 */
	public void closeConnection(Connection conn) throws SQLException;

	/**
	 * Does this connection provider support aggressive release of JDBC
	 * connections and re-acquisition of those connections (if need be) later?
	 * <p/>
	 * This is used in conjunction with {@link org.hibernate.cfg.Environment#RELEASE_CONNECTIONS}
	 * to aggressively release JDBC connections.  However, the configured ConnectionProvider
	 * must support re-acquisition of the same underlying connection for that semantic to work.
	 * <p/>
	 * Typically, this is only true in managed environments where a container
	 * tracks connections by transaction or thread.
	 *
	 * Note that JTA semantic depends on the fact that the underlying connection provider does
	 * support aggressive release.
	 *
	 * @return {@code true} if aggressive releasing is supported; {@code false} otherwise.
	 */
	public boolean supportsAggressiveRelease();
}
