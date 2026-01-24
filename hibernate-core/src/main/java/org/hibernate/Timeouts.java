/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.Timeout;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.jpa.internal.util.ConfigurationHelper;

import java.util.Map;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_LOCK_TIMEOUT;
import static org.hibernate.jpa.HibernateHints.HINT_TIMEOUT;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;

/**
 * Helpers for dealing with {@linkplain jakarta.persistence.Timeout timeout}
 * values, including some "magic values".
 *
 * @apiNote The {@link #NO_WAIT} and {@link #SKIP_LOCKED} magic values each
 * have a corresponding {@link LockMode} ({@link LockMode#UPGRADE_NOWAIT}
 * and {@link LockMode#UPGRADE_SKIPLOCKED}, respectively).
 *
 * @author Steve Ebersole
 *
 * @since 7.0
 */
@Incubating
public interface Timeouts {
	/**
	 * Timeout of 1 second.
	 */
	Timeout ONE_SECOND = Timeout.seconds( 1 );

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
		return switch ( timeoutInMilliseconds ) {
			case NO_WAIT_MILLI -> NO_WAIT;
			case WAIT_FOREVER_MILLI -> WAIT_FOREVER;
			case SKIP_LOCKED_MILLI -> SKIP_LOCKED;
			default -> Timeout.milliseconds( timeoutInMilliseconds );
		};
	}

	/**
	 * Whether the given timeout is one of the magic values - <ul>
	 *     <li>{@linkplain #NO_WAIT}
	 *     <li>{@linkplain #WAIT_FOREVER}
	 *     <li>{@linkplain #SKIP_LOCKED}
	 * </ul>
	 *
	 * @see #isMagicValue(int)
	 */
	static boolean isMagicValue(Timeout timeout) {
		return !isRealTimeout( timeout );
	}

	/**
	 * Whether the given value is one of the magic values - <ul>
	 *     <li>{@linkplain #NO_WAIT_MILLI}
	 *     <li>{@linkplain #WAIT_FOREVER_MILLI}
	 *     <li>{@linkplain #SKIP_LOCKED_MILLI}
	 * </ul>
	 *
	 * @see #isRealTimeout(int)
	 */
	static boolean isMagicValue(int millis) {
		return !isRealTimeout( millis );
	}

	/**
	 * Whether the timeout value is a real value, as opposed to one of the "magic values".
	 * Functionally, returns whether the {@linkplain Timeout#milliseconds() value} is greater than zero.
	 */
	static boolean isRealTimeout(Timeout timeout) {
		return timeout != null && isRealTimeout( timeout.milliseconds() );
	}

	/**
	 * Whether the timeout value is a real value, as opposed to one of the "magic values".
	 * Functionally, returns whether the value is greater than zero.
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

	static Integer getEffectiveTimeoutInSeconds(Timeout timeout) {
		return timeout == null ? null : getTimeoutInSeconds( timeout );
	}

	/**
	 * Get the number of (whole) seconds represented by the given {@code timeout}.
	 */
	static int getTimeoutInSeconds(int timeoutInMilliseconds) {
		// should never be negative here...
		assert timeoutInMilliseconds >= 0;
		return timeoutInMilliseconds == 0 ? 0 : Math.max( 1, Math.round( timeoutInMilliseconds / 1e3f ) );
	}

	static Timeout fromHints(Map<String, Object> properties) {
		var result = lockTimeoutFromHints( properties );
		if ( result == null ) {
			result = statementTimeoutFromHints( properties );
		}
		return result;
	}

	static Timeout lockTimeoutFromHints(Map<String, Object> properties) {
		var lockTimeoutRef = properties.get( HINT_SPEC_LOCK_TIMEOUT );
		if ( lockTimeoutRef == null ) {
			lockTimeoutRef = properties.get( JAKARTA_LOCK_TIMEOUT );
			if ( lockTimeoutRef != null ) {
				DeprecationLogger.DEPRECATION_LOGGER.deprecatedHint( JAKARTA_LOCK_TIMEOUT, HINT_SPEC_LOCK_TIMEOUT );
			}
		}
		return Timeouts.fromHintTimeout( lockTimeoutRef );
	}

	static Timeout statementTimeoutFromHints(Map<String, Object> properties) {
		var timeoutRef = properties.get( HINT_TIMEOUT );
		if ( timeoutRef == null ) {
			timeoutRef = properties.get( HINT_SPEC_QUERY_TIMEOUT );
		}
		if ( timeoutRef == null ) {
			timeoutRef = properties.get( HINT_JAVAEE_QUERY_TIMEOUT );
			if ( timeoutRef != null ) {
				DeprecationLogger.DEPRECATION_LOGGER.deprecatedHint( HINT_SPEC_QUERY_TIMEOUT, HINT_JAVAEE_QUERY_TIMEOUT );
			}
		}
		return Timeouts.fromHintTimeout( timeoutRef );
	}

	static Timeout fromHintTimeout(Object factoryHint) {
		if ( factoryHint == null ) {
			return WAIT_FOREVER;
		}
		if ( factoryHint instanceof Timeout timeout ) {
			return timeout;
		}
		if ( factoryHint instanceof Integer number ) {
			return Timeout.milliseconds( number );
		}
		return Timeout.milliseconds( Integer.parseInt( factoryHint.toString() ) );
	}

	/**
	 * @see org.hibernate.jpa.HibernateHints#HINT_TIMEOUT
	 */
	static Timeout fromHibernateHint(Object value) {
		// note: Hibernate defines timeout precision in seconds...
		if ( value == null ) {
			return null;
		}
		else if ( value instanceof Timeout ref ) {
			return ref;
		}
		else if ( value instanceof Number num ) {
			return Timeout.seconds( num.intValue() );
		}
		else {
			// try to convert it to an integer
			return Timeout.seconds( ConfigurationHelper.getInteger( value ) );
		}
	}

	/**
	 * @see org.hibernate.jpa.SpecHints#HINT_SPEC_QUERY_TIMEOUT
	 * @see org.hibernate.jpa.SpecHints#HINT_SPEC_LOCK_TIMEOUT
	 */
	static Timeout fromJpaHint(Object value) {
		// note: JPA defines timeout precision in milliseconds...
		if ( value == null ) {
			return null;
		}
		else if ( value instanceof Timeout ref ) {
			return ref;
		}
		else if ( value instanceof Number num ) {
			return Timeout.milliseconds( num.intValue() );
		}
		else {
			// try to convert it to an integer
			return Timeout.milliseconds( ConfigurationHelper.getInteger( value ) );
		}
	}

	static Timeout inSeconds(int timeout) {
		return null;
	}
}
