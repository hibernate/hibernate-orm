/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;

import java.sql.SQLException;

import org.hibernate.PessimisticLockException;
import org.hibernate.JDBCException;

/**
 * A {@link JDBCException} indicating a problem acquiring a lock
 * on the database.
 *
 * @apiNote Some databases make it quite hard to for a client to
 * distinguish a {@linkplain LockTimeoutException lock timeout}
 * from other sorts of rejected lock acquisitions, and so
 * application programs should not over-interpret the distinction
 * made between {@code LockAcquisitionException} and its subclass
 * {@link LockTimeoutException} on such platforms.
 *
 * @author Steve Ebersole
 */
public class LockAcquisitionException extends PessimisticLockException {
	public LockAcquisitionException(String string, SQLException root) {
		super( string, root );
	}

	public LockAcquisitionException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
