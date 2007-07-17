// $Id: JDBCConnectionException.java 4782 2004-11-21 00:11:27Z pgmjsd $
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * Implementation of JDBCException indicating problems with communicating with the
 * database (can also include incorrect JDBC setup).
 *
 * @author Steve Ebersole
 */
public class JDBCConnectionException extends JDBCException {
	public JDBCConnectionException(String string, SQLException root) {
		super( string, root );
	}

	public JDBCConnectionException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
