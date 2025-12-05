/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.Incubating;

/**
 * Indicates a Dialect's level of support for applying locks
 * to select statements with joins.
 *
 * @author Steve Ebersole
 */
@Incubating
public enum OuterJoinLockingType {
	/**
	 * Locks applied to joins are not supported, generally resulting in an
	 * error from the database.
	 */
	UNSUPPORTED,

	/**
	 * The database will not throw an exception, but the locking is ignored.
	 * This is the case with {@linkplain org.hibernate.dialect.HSQLDialect}
	 * and {@linkplain org.hibernate.dialect.H2Dialect}.
	 *
	 * @implSpec Should generally be treated the same as {@linkplain #UNSUPPORTED}
	 * since rows we expect to be locked not locked.
	 */
	IGNORED,

	/**
	 * The attempt to lock will succeed, but only root tables will be locked.
	 * In other words, the lock is not extended to joined tables.
	 *
	 * @implSpec Should generally be treated the same as {@linkplain #UNSUPPORTED}
	 * since rows we expect to be locked not locked.
	 */
	ROOT_ONLY,

	/**
	 * Applying locks to joined rows is supported, acquiring locks on all tables.
	 */
	FULL,

	/**
	 * Applying locks to joined rows is supported.  Which table rows are locked can be controlled
	 * per table reference, generally via one of:<ul>
	 *     <li>{@linkplain org.hibernate.dialect.RowLockStrategy#TABLE}
	 *     <li>{@linkplain org.hibernate.dialect.RowLockStrategy#COLUMN}
	 *     <li>{@linkplain org.hibernate.dialect.lock.PessimisticLockStyle#TABLE_HINT}
	 * </ul>
	 */
	IDENTIFIED
}
