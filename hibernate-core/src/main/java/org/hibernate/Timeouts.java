/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.Timeout;

/**
 * Magic {@linkplain jakarta.persistence.Timeout time out} values.
 *
 * @apiNote Generally speaking, for {@linkplain #NO_WAIT} and {@linkplain #SKIP_LOCKED},
 * the corresponding {@linkplain LockMode} should be preferred, but these magic values
 * allow use in conjunction with {@linkplain jakarta.persistence.LockModeType} without
 * the need to cast/unwrap to Hibernate specifics.
 *
 * @author Steve Ebersole
 *
 * @since 7.0
 */
public interface Timeouts {
	/**
	 * Raw magic millisecond value for {@linkplain #NO_WAIT}
	 */
	int NO_WAIT_MILLI = 0;

	/**
	 * Raw magic millisecond value for {@linkplain #WAIT_FOREVER}
	 */
	int WAIT_FOREVER_MILLI = -1;

	/**
	 * Raw magic millisecond value for {@linkplain #SKIP_LOCKED}
	 */
	int SKIP_LOCKED_MILLI = -2;

	/**
	 * Indicates that the database should not wait at all to acquire
	 * a pessimistic lock which is not immediately available.
	 *
	 * @see LockMode#UPGRADE_NOWAIT
	 */
	Timeout NO_WAIT = Timeout.milliseconds( NO_WAIT_MILLI );

	/**
	 * Indicates that there is no timeout for the lock acquisition,
	 * that is, that the database should in principle wait forever
	 * to obtain the lock.
	 */
	Timeout WAIT_FOREVER = Timeout.milliseconds( WAIT_FOREVER_MILLI );

	/**
	 * Indicates that rows which are already locked should be skipped.
	 *
	 * @see LockMode#UPGRADE_SKIPLOCKED
	 */
	Timeout SKIP_LOCKED = Timeout.milliseconds( SKIP_LOCKED_MILLI );
}
