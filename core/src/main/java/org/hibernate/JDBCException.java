//$Id: JDBCException.java 4626 2004-09-27 15:24:38Z oneovthafew $
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
