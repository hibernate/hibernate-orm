/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * A {@link JDBCException} indicating a transaction failed because it could not be placed into a serializable ordering
 * among all currently-executing transactions
 *
 * @apiNote At present, this is only used to represent {@code WriteTooOldError} on CockroachDB.
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
