package org.hibernate.jdbc;

import java.sql.Connection;

/**
 * Interface implemented by JDBC connection wrappers in order to give
 * access to the underlying wrapped connection.
 *
 * @author Steve Ebersole
 */
public interface ConnectionWrapper {
	/**
	 * Get a reference to the wrapped connection.
	 *
	 * @return The wrapped connection.
	 */
	public Connection getWrappedConnection();
}
