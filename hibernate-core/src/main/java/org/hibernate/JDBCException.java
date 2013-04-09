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
	 * Constructs a JDBCException using the given information
	 *
	 * @param message The message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public JDBCException(String message, SQLException cause) {
		this( message, cause, null );
	}

	public JDBCException(String string, SQLException cause, String sql) {
		super( string, cause );
		this.sqlException = cause;
		this.sql = sql;
	}

	/**
	 * Get the SQLState of the underlying <tt>SQLException</tt>.
	 * @see java.sql.SQLException
	 * @return String
	 */
	public String getSQLState() {
		return sqlException.getSQLState();
	}

	/**
	 * Get the <tt>errorCode</tt> of the underlying <tt>SQLException</tt>.
	 * @see java.sql.SQLException
	 * @return int the error code
	 */
	public int getErrorCode() {
		return sqlException.getErrorCode();
	}

	/**
	 * Get the underlying <tt>SQLException</tt>.
	 * @return SQLException
	 */
	public SQLException getSQLException() {
		return sqlException;
	}
	
	/**
	 * Get the actual SQL statement that caused the exception
	 * (may be null)
	 */
	public String getSQL() {
		return sql;
	}

}
