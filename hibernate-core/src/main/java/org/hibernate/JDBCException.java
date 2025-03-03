/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.SQLException;

/**
 * Wraps a {@link SQLException} arising from the JDBC driver.
 * Indicates that an error occurred during a JDBC call.
 *
 * @author Gavin King
 *
 * @see SQLException
 */
public class JDBCException extends HibernateException {
	private final SQLException sqlException;
	private final String message;
	private final String sql;

	/**
	 * Constructs a {@code JDBCException} using the given information.
	 *
	 * @param message The message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public JDBCException(String message, SQLException cause) {
		super( message, cause );
		this.message = message;
		this.sqlException = cause;
		this.sql = null;
	}

	/**
	 * Constructs a {@code JDBCException} using the given information.
	 *
	 * @param message The message explaining the exception condition
	 * @param cause The underlying cause
	 * @param sql The sql being executed when the exception occurred
	 */
	public JDBCException(String message, SQLException cause, String sql) {
		super( sql == null ? message : message + " [" + sql + "]", cause );
		this.message = message;
		this.sqlException = cause;
		this.sql = sql;
	}

	/**
	 * Get the X/Open or ANSI SQL SQLState error code from the underlying {@link SQLException}.
	 *
	 * @return The X/Open or ANSI SQL SQLState error code; may return null.
	 *
	 * @see SQLException#getSQLState()
	 */
	public String getSQLState() {
		return sqlException.getSQLState();
	}

	/**
	 * Get the vendor specific error code from the underlying {@link SQLException}.
	 *
	 * @return The vendor specific error code
	 *
	 * @see SQLException#getErrorCode()
	 */
	public int getErrorCode() {
		return sqlException.getErrorCode();
	}

	/**
	 * Get the underlying {@link SQLException}.
	 *
	 * @return The SQLException
	 */
	public SQLException getSQLException() {
		return sqlException;
	}

	/**
	 * Get the actual SQL statement being executed when the exception occurred.
	 *
	 * @return The SQL statement; may return null.
	 */
	public String getSQL() {
		return sql;
	}

	/**
	 * @return The error message without the SQL statement appended
	 */
	public String getErrorMessage() {
		return message;
	}
}
