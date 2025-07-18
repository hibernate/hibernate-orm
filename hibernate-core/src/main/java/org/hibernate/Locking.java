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
public interface Locking {
	/**
	 * When pessimistic locking is requested, this enum defines
	 * what exactly will be locked.
	 *
	 * @apiNote Same intention as the JPA {@linkplain PessimisticLockScope},
	 * but offering the additional {@linkplain #INCLUDE_FETCHES} behavior.
	 *
	 * @see FollowOn
	 */
	enum Scope implements FindOption, LockOption, RefreshOption {
		/**
		 * Lock the database row(s) that correspond to the non-collection-valued
		 * persistent state of that instance. If a joined inheritance strategy is
		 * used, or if the entity is otherwise mapped to a secondary table, this
		 * entails locking the row(s) for the entity instance in the additional table(s).
		 *
		 * @see PessimisticLockScope#NORMAL
		 */
		ROOT_ONLY,

		/**
		 * In addition to the locking behavior specified for {@linkplain #ROOT_ONLY},
		 * rows for collection tables ({@linkplain jakarta.persistence.ElementCollection},
		 * {@linkplain jakarta.persistence.OneToMany} and {@linkplain jakarta.persistence.ManyToMany})
		 * will also be locked.
		 * <p/>
		 * Hibernate will only lock these collection rows when they are joined.  The alternatives
		 * would be to either:<ul>
		 *     <li>
		 *         Add joins for the collection tables, which is problematic because
		 *         <ul>
		 *             <li>if {@code inner joins} are added, the results will be unexpectedly affected</li>
		 *             <li>if {@code outer joins} are added, many databases do not allow locking outer joins</li>
		 *         </ul>
		 *     </li>
		 *     <li>
		 *         Perform {@linkplain FollowOn follow-on} locking which can lead to deadlocks.
		 *     </li>
		 * </ul>
		 *
		 * @apiNote Locking only joined rows is arguably not fully compliant with the specification.
		 * However, we believe it is the best implementation.
		 *
		 * @see PessimisticLockScope#EXTENDED
		 */
		INCLUDE_COLLECTIONS,

		/**
		 * All tables with fetched rows will be locked.
		 *
		 * @apiNote This is Hibernate's legacy behavior, and has no
		 * corresponding JPA scope.
		 */
		INCLUDE_FETCHES;

		/**
		 * The JPA {@linkplain PessimisticLockScope} which corresponds to this Scope.
		 *
		 * @return The corresponding PessimisticLockScope, or {@code null}.
		 */
		public PessimisticLockScope getCorrespondingJpaScope() {
			return switch (this) {
				case ROOT_ONLY -> PessimisticLockScope.NORMAL;
				case INCLUDE_COLLECTIONS -> PessimisticLockScope.EXTENDED;
				case INCLUDE_FETCHES -> null;
			};
		}

		public static Scope fromJpaScope(PessimisticLockScope scope) {
			if ( scope == PessimisticLockScope.EXTENDED ) {
				return INCLUDE_COLLECTIONS;
			}
			// null, NORMAL
			return ROOT_ONLY;
		}

		public static Scope interpret(String name) {
			if ( name == null ) {
				return null;
			}
			return valueOf( name.toUpperCase( Locale.ROOT ) );
		}
	}

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
}
