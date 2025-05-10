/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.Timeout;

/**
 * Helpers for dealing with {@linkplain jakarta.persistence.Timeout time out} values,
 * including some "magic values".
 *
 * @apiNote The {@linkplain #NO_WAIT} and {@linkplain #SKIP_LOCKED} magic values have
 * special {@linkplain LockMode} values as well ({@linkplain LockMode#UPGRADE_NOWAIT}
 * and {@linkplain LockMode#UPGRADE_SKIPLOCKED}, respectively).
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
	 * @see #NO_WAIT_MILLI
	 * @see LockMode#UPGRADE_NOWAIT
	 */
	Timeout NO_WAIT = Timeout.milliseconds( NO_WAIT_MILLI );

	/**
	 * Indicates that there is no timeout for the lock acquisition,
	 * that is, that the database should in principle wait forever
	 * to obtain the lock.
	 *
	 * @see #WAIT_FOREVER_MILLI
	 */
	Timeout WAIT_FOREVER = Timeout.milliseconds( WAIT_FOREVER_MILLI );

	/**
	 * Indicates that rows which are already locked should be skipped.
	 *
	 * @see #SKIP_LOCKED_MILLI
	 * @see LockMode#UPGRADE_SKIPLOCKED
	 */
	Timeout SKIP_LOCKED = Timeout.milliseconds( SKIP_LOCKED_MILLI );

	/**
	 * Similar to simply calling {@linkplain Timeout#milliseconds(int)}, but accounting for
	 * the "magic values".
	 */
	static Timeout interpretMilliSeconds(int timeoutInMilliseconds) {
		if ( timeoutInMilliseconds == NO_WAIT_MILLI ) {
			return NO_WAIT;
		}
		else if ( timeoutInMilliseconds == WAIT_FOREVER_MILLI ) {
			return WAIT_FOREVER;
		}
		else if ( timeoutInMilliseconds == SKIP_LOCKED_MILLI ) {
			return SKIP_LOCKED;
		}
		return Timeout.milliseconds( timeoutInMilliseconds );
	}

	/**
	 * Is the timeout value a real value, as opposed to one of the
	 * "magic values".  Functionally, returns whether the value is
	 * greater than zero.
	 */
	static boolean isRealTimeout(Timeout timeout) {
		return isRealTimeout( timeout.milliseconds() );
	}

	/**
	 * Is the timeout value a real value, as opposed to one of the
	 * "magic values".  Functionally, returns whether the value is
	 * greater than zero.
	 */
	static boolean isRealTimeout(int timeoutInMilliseconds) {
		return timeoutInMilliseconds > 0;
	}

	/**
	 * Get the number of (whole) seconds represented by the given {@code timeout}.
	 */
	static int getTimeoutInSeconds(Timeout timeout) {
		return getTimeoutInSeconds( timeout.milliseconds() );
	}

	/**
	 * Get the number of (whole) seconds represented by the given {@code timeout}.
	 */
	static int getTimeoutInSeconds(int timeoutInMilliseconds) {
		// should never be negative here...
		assert timeoutInMilliseconds >= 0;
		return timeoutInMilliseconds == 0 ? 0 : Math.max( 1, Math.round( timeoutInMilliseconds / 1e3f ) );
	}
}
