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
 * @author Steve Ebersole
 *
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
