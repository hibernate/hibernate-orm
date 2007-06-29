// $Id: GenericJDBCException.java 4782 2004-11-21 00:11:27Z pgmjsd $
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * Generic, non-specific JDBCException.
 *
 * @author Steve Ebersole
 */
public class GenericJDBCException extends JDBCException {
	public GenericJDBCException(String string, SQLException root) {
		super( string, root );
	}

	public GenericJDBCException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
