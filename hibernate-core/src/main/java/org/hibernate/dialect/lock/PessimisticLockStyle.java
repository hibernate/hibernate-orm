/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;

/**
 * Indicates how a dialect supports acquiring pessimistic locks
 * as part of a {@code SELECT} statement.
 *
 * @author Steve Ebersole
 */
public enum PessimisticLockStyle {
	/**
	 * The dialect does not support pessimistic locking.
	 */
	NONE,

	/**
	 * The dialect supports pessimistic locking through locking
	 * clause such as {@code FOR UPDATE (OF)} or {@code FOR SHARE (OF)}.
	 */
	CLAUSE,

	/**
	 * The dialect supports pessimistic locking through Transact-SQL style
	 * table locking hints, associated with the table references in the
	 * {@code FROM} clause.
	 *
	 * @see Dialect#appendLockHint(LockOptions, String)
	 */
	TABLE_HINT
}
