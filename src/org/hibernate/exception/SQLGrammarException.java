// $Id$
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * Implementation of JDBCException indicating that the SQL sent to the database
 * server was invalid (syntax error, invalid object references, etc).
 *
 * @author Steve Ebersole
 */
public class SQLGrammarException extends JDBCException {
	/**
	 * Constructor for JDBCException.
	 *
	 * @param root The underlying exception.
	 */
	public SQLGrammarException(String message, SQLException root) {
		super( message, root );
	}

	/**
	 * Constructor for JDBCException.
	 *
	 * @param message Optional message.
	 * @param root    The underlying exception.
	 */
	public SQLGrammarException(String message, SQLException root, String sql) {
		super( message, root, sql );
	}
}
