/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * A {@link JDBCException} indicating that a {@linkplain org.hibernate.LockMode lock}
 * request timed out on the database.
 *
 * @apiNote Some databases make it quite hard to for a client
 * to distinguish a lock timeout from other sorts of rejected
 * lock acquisitions, and so application programs should avoid
 * over-interpreting the distinction made between
 * {@code LockTimeoutException} and its superclass
 * {@link LockAcquisitionException} on such platforms.
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.Timeout
 * @see org.hibernate.LockOptions#getTimeOut
 * @see org.hibernate.LockOptions#setTimeOut
 * @see jakarta.persistence.LockTimeoutException
 */
public class LockTimeoutException extends LockAcquisitionException {
	public LockTimeoutException(String string, SQLException root) {
		super( string, root );
	}

	public LockTimeoutException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
