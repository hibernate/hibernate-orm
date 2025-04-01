/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.SQLException;

/**
 * Thrown when a pessimistic locking conflict occurs.
 *
 * @apiNote When a conflict is detected while acquiring a database-level lock,
 * {@link org.hibernate.exception.LockAcquisitionException} is preferred.
 *
 * @author Scott Marlow
 *
 * @see jakarta.persistence.PessimisticLockException
 * @see org.hibernate.exception.LockAcquisitionException
 */
public class PessimisticLockException extends JDBCException {
	/**
	 * Constructs a {@code PessimisticLockException} using the specified information.
	 *
	 * @param message A message explaining the exception condition
	 * @param sqlException The underlying SQL exception
	 * @param sql The sql that led to the exception (possibly null, but usually not)
	 */
	public PessimisticLockException(String message, SQLException sqlException, String sql) {
		super( message, sqlException, sql );
	}
	/**
	 * Constructs a {@code PessimisticLockException} using the specified information.
	 *
	 * @param message A message explaining the exception condition
	 * @param sqlException The underlying SQL exception
	 */
	public PessimisticLockException(String message, SQLException sqlException) {
		super( message, sqlException );

	}
}
