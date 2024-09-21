/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * A {@link JDBCException} indicating that a lock request
 * timed out on the database.
 *
 * @author Steve Ebersole
 */
public class LockTimeoutException extends LockAcquisitionException {
	public LockTimeoutException(String string, SQLException root) {
		super( string, root );
	}

	public LockTimeoutException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
