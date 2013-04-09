/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate;

import java.sql.SQLException;

/**
 * Wraps a {@link SQLException}.  Indicates that an exception occurred during a JDBC call.
 *
 * @author Gavin King
 *
 * @see java.sql.SQLException
 */
public class JDBCException extends HibernateException {
	private final SQLException sqlException;
	private final String sql;

	/**
	 * Constructs a JDBCException using the given information.
	 *
	 * @param message The message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public JDBCException(String message, SQLException cause) {
		this( message, cause, null );
	}

	/**
	 * Constructs a JDBCException using the given information.
	 *
	 * @param message The message explaining the exception condition
	 * @param cause The underlying cause
	 * @param sql The sql being executed when the exception occurred
	 */
	public JDBCException(String message, SQLException cause, String sql) {
		super( message, cause );
		this.sqlException = cause;
		this.sql = sql;
	}

	/**
	 * Get the X/Open or ANSI SQL SQLState error code from the underlying {@link SQLException}.
	 *
	 * @return The X/Open or ANSI SQL SQLState error code; may return null.
	 *
	 * @see java.sql.SQLException#getSQLState()
	 */
	public String getSQLState() {
		return sqlException.getSQLState();
	}

	/**
	 * Get the vendor specific error code from the underlying {@link SQLException}.
	 *
	 * @return The vendor specific error code
	 *
	 * @see java.sql.SQLException#getErrorCode()
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

}
