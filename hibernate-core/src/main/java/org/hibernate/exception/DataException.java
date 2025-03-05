/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;

import java.sql.SQLException;

import org.hibernate.JDBCException;

/**
 * A {@link JDBCException} indicating that evaluation of the
 * valid SQL statement against the given data resulted in some
 * illegal operation, mismatched types or incorrect cardinality.
 *
 * @author Gavin King
 */
public class DataException extends JDBCException {
	/**
	 * Constructor for DataException.
	 *
	 * @param root The underlying exception.
	 */
	public DataException(String message, SQLException root) {
		super( message, root );
	}

	/**
	 * Constructor for DataException.
	 *
	 * @param message Optional message.
	 * @param root    The underlying exception.
	 */
	public DataException(String message, SQLException root, String sql) {
		super( message, root, sql );
	}
}
