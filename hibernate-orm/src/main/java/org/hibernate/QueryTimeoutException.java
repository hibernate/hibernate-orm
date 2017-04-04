/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.sql.SQLException;

/**
 * Thrown when a database query timeout occurs.
 *
 * @author Scott Marlow
 */
public class QueryTimeoutException extends JDBCException {
	/**
	 * Constructs a QueryTimeoutException using the supplied information.
	 *
	 * @param message The message explaining the exception condition
	 * @param sqlException The underlying SQLException
	 * @param sql The sql being executed when the exception occurred.
	 */
	public QueryTimeoutException(String message, SQLException sqlException, String sql) {
		super( message, sqlException, sql );
	}
}
