/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.FindOption;
import jakarta.persistence.LockOption;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.Timeout;
import org.hibernate.dialect.Dialect;

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
		ROOT_ONLY( PessimisticLockScope.NORMAL ),

		/**
		 * In addition to the locking behavior specified for {@linkplain #ROOT_ONLY},
		 * rows for collection tables ({@linkplain jakarta.persistence.ElementCollection},
		 * {@linkplain jakarta.persistence.OneToMany} and {@linkplain jakarta.persistence.ManyToMany})
		 * will also be locked.
		 *
		 * @see PessimisticLockScope#EXTENDED
		 */
		INCLUDE_COLLECTIONS( PessimisticLockScope.EXTENDED ),

		/**
		 * All tables with fetched rows will be locked.
		 *
		 * @apiNote This is Hibernate's legacy behavior, and has no
		 * corresponding JPA scope.
		 */
		INCLUDE_FETCHES( null );

		private final PessimisticLockScope jpaScope;

		Scope(PessimisticLockScope jpaScope) {
			this.jpaScope = jpaScope;
		}

		/**
		 * The JPA PessimisticLockScope which corresponds to this LockScope.
		 *
		 * @return The corresponding PessimisticLockScope, or {@code null}.
		 */
		public PessimisticLockScope getCorrespondingJpaScope() {
			return jpaScope;
		}

		public static Scope fromJpaScope(PessimisticLockScope scope) {
			if ( scope == PessimisticLockScope.EXTENDED ) {
				return INCLUDE_COLLECTIONS;
			}
			// null, NORMAL
			return ROOT_ONLY;
		}
	}

	/**
	 * In certain circumstances, Hibernate may need to acquire locks
	 * through the use of subsequent queries.  For example, some
	 * databases may not like attempting to lock rows in a join or
	 * queries with paging.  In such cases, Hibernate will fall back
	 * to issuing additional lock queries to lock some of the rows.
	 * This option controls whether Hibernate is allowed to use
	 * this approach.
	 */
	enum FollowOn implements FindOption, LockOption, RefreshOption {
		/**
		 * Allow Hibernate to perform follow-on locking when it needs to.
		 */
		ALLOW,

		/**
		 * Do not allow Hibernate to perform follow-on locking when it needs to, throwing
		 * an exception instead.
		 */
		DISALLOW,

		/**
		 * Do not allow Hibernate to perform follow-on locking when it needs to, but just ignore
		 * the situation.
		 *
		 * @apiNote This can lead to rows not being locked when they are expected to be.
		 */
		IGNORE
	}

	/**
	 * How locked rows should be handled with pessimistic lock attempts.
	 * The default is to {@linkplain #WAIT wait} for the locks to be released.
	 */
	enum LockedRows implements FindOption, LockOption, RefreshOption {
		/**
		 * The default.  The transaction will wait for the row locks to
		 * be released, within any specified {@linkplain Timeout timeout}.
		 */
		WAIT,

		/**
		 * Immediately skips locked rows.
		 *
		 * @apiNote Only legal if the database
		 * {@linkplain Dialect#supportsSkipLocked() supports skipping locked rows}.
		 */
		SKIP
	}
}
