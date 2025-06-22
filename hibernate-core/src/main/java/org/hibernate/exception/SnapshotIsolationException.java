/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;

import org.hibernate.Incubating;
import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * A {@link org.hibernate.JDBCException} indicating that a request failed due to snapshot isolation.
 * This is a condition which indicates an <em>optimistic</em> failure.
 *
 * @apiNote At present, this is only used to represent {@code DB_RECORD_CHANGED} on MariaDB.
 *
 * @see jakarta.persistence.OptimisticLockException
 */
@Incubating
public class SnapshotIsolationException extends JDBCException {
	public SnapshotIsolationException(String string, SQLException root, String sql) {
		super( string, root, sql );
	}
}
