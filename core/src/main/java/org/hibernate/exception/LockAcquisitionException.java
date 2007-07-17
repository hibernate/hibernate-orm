// $Id: LockAcquisitionException.java 4782 2004-11-21 00:11:27Z pgmjsd $
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * Implementation of JDBCException indicating a problem acquiring lock
 * on the database.
 *
 * @author Steve Ebersole
 */
public class LockAcquisitionException extends JDBCException {
	public LockAcquisitionException(String string, SQLException root) {
		super( string, root );
	}

	public LockAcquisitionException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
