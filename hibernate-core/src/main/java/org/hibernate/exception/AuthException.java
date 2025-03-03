/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * A {@link JDBCException} indicating an authentication or authorization failure.
 *
 * @since 7.0
 *
 * @author Gavin King
 */
public class AuthException extends JDBCException {
	/**
	 * Constructor for AuthException.
	 *
	 * @param root The underlying exception.
	 */
	public AuthException(String message, SQLException root) {
		super( message, root );
	}

	/**
	 * Constructor for AuthException.
	 *
	 * @param message Optional message.
	 * @param root    The underlying exception.
	 */
	public AuthException(String message, SQLException root, String sql) {
		super( message, root, sql );
	}
}
