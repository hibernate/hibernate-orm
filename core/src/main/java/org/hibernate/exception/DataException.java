// $Id: DataException.java 8062 2005-09-01 15:41:46Z oneovthafew $
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * Implementation of JDBCException indicating that evaluation of the
 * valid SQL statement against the given data resulted in some
 * illegal operation, mismatched types or incorrect cardinality.
 *
 * @author Gavin King
 */
public class DataException extends JDBCException {
	/**
	 * Constructor for JDBCException.
	 *
	 * @param root The underlying exception.
	 */
	public DataException(String message, SQLException root) {
		super( message, root );
	}

	/**
	 * Constructor for JDBCException.
	 *
	 * @param message Optional message.
	 * @param root    The underlying exception.
	 */
	public DataException(String message, SQLException root, String sql) {
		super( message, root, sql );
	}
}
