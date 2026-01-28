/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.FindOption;
import jakarta.persistence.LockOption;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.RefreshOption;

import java.util.Locale;

/**
 * Support for various aspects of pessimistic locking.
 *
 * @see LockMode#PESSIMISTIC_READ
 * @see LockMode#PESSIMISTIC_WRITE
 * @see LockMode#PESSIMISTIC_FORCE_INCREMENT
 *
 * @author Steve Ebersole
 */
@Incubating
public interface Locking {

	/**
	 * In certain circumstances, Hibernate may need to acquire locks
	 * through the use of additional queries.  For example, some
	 * databases may not allow locking rows for queries with a join or
	 * with pagination.  In such cases, Hibernate will fall back
	 * to issuing additional queries to lock the matching rows.
	 * This option controls whether Hibernate is allowed to use
	 * this approach.
	 */
	enum FollowOn implements FindOption, LockOption, RefreshOption {
		/**
		 * Allow follow-on locking when necessary.
		 */
		ALLOW,

		/**
		 * Disallow follow-on locking, throwing an exception instead.
		 */
		DISALLOW,

		/**
		 * Disallow follow-on locking, but ignoring the situation
		 * rather than throw an exception.
		 *
		 * @apiNote This can lead to rows not being locked
		 * when they are expected to be.
		 *
		 * @see #DISALLOW
		 */
		IGNORE,

		/**
		 * Force the use of follow-on locking.
		 *
		 * @apiNote This may lead to exceptions from the database.
		 */
		FORCE;

		public static FollowOn interpret(String name) {
			if ( name == null ) {
				return null;
			}

			return valueOf( name.toUpperCase( Locale.ROOT ) );
		}

		public static FollowOn fromHint(Object value) {
			if ( value == null ) {
				return Locking.FollowOn.ALLOW;
			}
			else if ( value instanceof Locking.FollowOn strategyValue ) {
				return strategyValue;
			}
			else {
				return Locking.FollowOn.valueOf( value.toString() );
			}
		}

		/**
		 * Interprets the follow-on strategy into the legacy boolean values.
		 *
		 * @return {@code true} if {@linkplain #FORCE}; {@code false} if {@linkplain #DISALLOW};
		 * {@code null} otherwise.
		 *
		 * @see #fromLegacyValue
		 */
		public Boolean asLegacyValue() {
			return switch ( this ) {
				case FORCE -> true;
				case DISALLOW -> false;
				default -> null;
			};
		}

		/**
		 * Given a legacy boolean value, interpret the follow-on strategy.
		 *
		 * @return {@linkplain #FORCE} if {@code true};
		 * {@linkplain #DISALLOW} if {@code false};
		 * {@linkplain #ALLOW} otherwise.
		 *
		 * @see #asLegacyValue()
		 */
		public static FollowOn fromLegacyValue(Boolean value) {
			if ( value == Boolean.TRUE ) {
				return FORCE;
			}
			if ( value == Boolean.FALSE ) {
				return DISALLOW;
			}
			return ALLOW;
		}
	}

	static PessimisticLockScope scopeFromHint(Object value) {
		if ( value == null ) {
			return PessimisticLockScope.NORMAL;
		}
		else if ( value instanceof PessimisticLockScope ref ) {
			return ref;
		}
		else {
			var name = value.toString().toUpperCase( Locale.ROOT );
			return PessimisticLockScope.valueOf( name );
		}
	}
}
