/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception;
import java.sql.SQLException;

import org.hibernate.JDBCException;

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
