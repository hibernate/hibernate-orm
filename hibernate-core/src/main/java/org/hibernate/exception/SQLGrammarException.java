/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;

import java.sql.SQLException;

import org.hibernate.JDBCException;

/**
 * Specialization of {@link JDBCException} indicating that the SQL sent to the
 * database server was invalid, either due to a syntax error, unrecognized name,
 * or similar problem.
 * <p>
 * The name of this class is misleading: the SQL might be syntactically well-formed.
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
