/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.SQLException;

/**
 * A {@link JDBCException} indicating that a database query timed
 * out on the database.
 *
 * @author Scott Marlow
 *
 * @see jakarta.persistence.Query#setTimeout
 * @see org.hibernate.query.CommonQueryContract#setTimeout
 * @see jakarta.persistence.QueryTimeoutException
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
