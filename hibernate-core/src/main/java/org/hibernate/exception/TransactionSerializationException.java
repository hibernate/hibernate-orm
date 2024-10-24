/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;
/**
 * A {@link JDBCException} indicating a transaction failed because it could not be placed into a serializable ordering
 * among all of the currently-executing transactions
 *
 * @author Karel Maesen
 */
public class TransactionSerializationException extends JDBCException {
	public TransactionSerializationException(String message, SQLException cause) {
		super( message, cause );
	}

	public TransactionSerializationException(String message, SQLException cause, String sql) {
		super( message, cause, sql );
	}
}
