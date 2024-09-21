/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.SQLException;

/**
 * Thrown when a pessimistic locking conflict occurs.
 *
 * @author Scott Marlow
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
}
