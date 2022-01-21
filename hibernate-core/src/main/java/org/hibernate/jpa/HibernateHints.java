/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa;

import org.hibernate.FlushMode;
import org.hibernate.query.Query;

/**
 * List of Hibernate-specific (extension) hints available to query,
 * load and lock scenarios.
 *
 * Some hints are only valid for certain scenarios, which is noted on
 * each constant's docuementation
 *
 * @author Steve Ebersole
 */
public interface HibernateHints {
	/**
	 * Hint for specifying a query timeout, in seconds.
	 *
	 * @see org.hibernate.query.Query#setTimeout
	 * @see java.sql.Statement#setQueryTimeout
	 */
	String HINT_TIMEOUT = "org.hibernate.timeout";

	/**
	 * Hint for specifying that objects loaded into the persistence
	 * context as a result of a query should be associated with the
	 * persistence context as read-only.
	 *
	 * @see Query#setReadOnly
	 */
	String HINT_READ_ONLY = "org.hibernate.readOnly";

	/**
	 * Hint for specifying a JDBC fetch size to be applied to the
	 * JDBC statement.
	 *
	 * @see Query#setFetchSize
	 * @see java.sql.Statement#setFetchSize
	 */
	String HINT_FETCH_SIZE = "org.hibernate.fetchSize";

	/**
	 * Hint for specifying a database comment to be applied to
	 * the SQL sent to the database.
	 *
	 * @implSpec Not valid for {@link org.hibernate.procedure.ProcedureCall}
	 * nor {@link jakarta.persistence.StoredProcedureQuery} scenarios
	 *
	 * @see Query#setComment
	 */
	String HINT_COMMENT = "org.hibernate.comment";

	/**
	 * Hint for specifying the {@link org.hibernate.FlushMode}
	 * to apply to Query execution
	 *
	 * @see Query#setHibernateFlushMode
	 * @see org.hibernate.Session#setHibernateFlushMode
	 */
	String HINT_FLUSH_MODE = "org.hibernate.flushMode";

	/**
	 * Hint for specifying whether results from a query should
	 * be stored in the query cache
	 *
	 * @see Query#setCacheable
	 */
	String HINT_CACHEABLE = "org.hibernate.cacheable";

	/**
	 * Hint for specifying the region of the query cache into which
	 * the results should be stored
	 *
	 * @implSpec No effect unless {@link #HINT_CACHEABLE} is set to {@code true}
	 *
	 * @see Query#setCacheRegion
	 */
	String HINT_CACHE_REGION = "org.hibernate.cacheRegion";

	/**
	 * Hint for specifying the {@link org.hibernate.CacheMode} to use
	 *
	 * @implSpec No effect unless {@link #HINT_CACHEABLE} is set to {@code true}
	 *
	 * @see Query#setCacheMode
	 */
	String HINT_CACHE_MODE = "org.hibernate.cacheMode";

	/**
	 * Hint to enable/disable the follow-on-locking mechanism provided by
	 * {@link org.hibernate.dialect.Dialect#useFollowOnLocking(String, org.hibernate.query.spi.QueryOptions)}.
	 * A value of {@code true} enables follow-on-locking, whereas a value of
	 * {@code false} disables it. If the value is {@code null}, the
	 * {@code Dialect}'s default strategy is used.
	 *
	 * @since 5.2
	 */
	String HINT_FOLLOW_ON_LOCKING = "hibernate.query.followOnLocking";

	/**
	 * Hint for specifying the lock-mode to apply to the results from a
	 * native-query.
	 *
	 * While Hibernate supports applying lock-mode to a natove-query, the specification
	 * requires that {@link jakarta.persistence.Query#setLockMode} throw an
	 * {@link IllegalStateException} if called for a native query.
	 *
	 * Accepts a {@link jakarta.persistence.LockModeType} or a {@link org.hibernate.LockMode}
	 */
	String HINT_NATIVE_LOCK_MODE = "org.hibernate.lockMode";

	/**
	 * Hint for specifying query spaces to be applied to a NativeQuery.
	 *
	 * Passed value can be any of:<ul>
	 *     <li>List of the spaces</li>
	 *     <li>array of the spaces</li>
	 *     <li>String as "whitespace"-separated list of the spaces</li>
	 * </ul>
	 *
	 * Note that the passed space need not match any real spaces/tables in
	 * the underlying query.  This can be used to completely circumvent
	 * the auto-flush checks as well as any cache invalidation that might
	 * occur as part of a flush.  See {@link org.hibernate.query.SynchronizeableQuery}
	 * and {@link FlushMode#MANUAL} for more information.
	 *
	 * @see org.hibernate.SynchronizeableQuery
	 * @see #HINT_FLUSH_MODE
	 */
	String HINT_NATIVE_SPACES = "org.hibernate.query.native.spaces";

	/**
	 * Whether to treat a {@link org.hibernate.procedure.ProcedureCall}
	 * or {@link jakarta.persistence.StoredProcedureQuery} as a call
	 * to a function rather than a call to a procedure
	 */
	String HINT_CALLABLE_FUNCTION = "org.hibernate.callableFunction";
}
