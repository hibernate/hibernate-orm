/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * Constructs a {@code QueryTimeoutException} using the supplied information.
	 *
	 * @param message The message explaining the exception condition
	 * @param sqlException The underlying SQLException
	 * @param sql The sql being executed when the exception occurred.
	 */
	public QueryTimeoutException(String message, SQLException sqlException, String sql) {
		super( message, sqlException, sql );
	}
}
