// $Id: ConstraintViolationException.java 4782 2004-11-21 00:11:27Z pgmjsd $
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * Implementation of JDBCException indicating that the requested DML operation
 * resulted in a violation of a defined integrity constraint.
 *
 * @author Steve Ebersole
 */
public class ConstraintViolationException extends JDBCException {

	private String constraintName;

	public ConstraintViolationException(String message, SQLException root, String constraintName) {
		super( message, root );
		this.constraintName = constraintName;
	}

	public ConstraintViolationException(String message, SQLException root, String sql, String constraintName) {
		super( message, root, sql );
		this.constraintName = constraintName;
	}

	/**
	 * Returns the name of the violated constraint, if known.
	 *
	 * @return The name of the violated constraint, or null if not known.
	 */
	public String getConstraintName() {
		return constraintName;
	}
}
