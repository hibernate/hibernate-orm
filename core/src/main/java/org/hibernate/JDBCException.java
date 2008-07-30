/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate;

import java.sql.SQLException;


/**
 * Wraps an <tt>SQLException</tt>. Indicates that an exception
 * occurred during a JDBC call.
 *
 * @see java.sql.SQLException
 * @author Gavin King
 */
public class JDBCException extends HibernateException {

	private SQLException sqle;
	private String sql;

	public JDBCException(String string, SQLException root) {
		super(string, root);
		sqle=root;
	}

	public JDBCException(String string, SQLException root, String sql) {
		this(string, root);
		this.sql = sql;
	}

	/**
	 * Get the SQLState of the underlying <tt>SQLException</tt>.
	 * @see java.sql.SQLException
	 * @return String
	 */
	public String getSQLState() {
		return sqle.getSQLState();
	}

	/**
	 * Get the <tt>errorCode</tt> of the underlying <tt>SQLException</tt>.
	 * @see java.sql.SQLException
	 * @return int the error code
	 */
	public int getErrorCode() {
		return sqle.getErrorCode();
	}

	/**
	 * Get the underlying <tt>SQLException</tt>.
	 * @return SQLException
	 */
	public SQLException getSQLException() {
		return sqle;
	}
	
	/**
	 * Get the actual SQL statement that caused the exception
	 * (may be null)
	 */
	public String getSQL() {
		return sql;
	}

}
