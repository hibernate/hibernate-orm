/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.FlushMode;

/**
 * Consolidation of hints available to Hibernate JPA queries.  Mainly used to define features available on
 * Hibernate queries that have no corollary in JPA queries.
 */
public class QueryHints {
	/**
	 * Disallow instantiation.
	 */
	private QueryHints() {
	}

	/**
	 * The cache mode to use.
	 *
	 * @see org.hibernate.query.Query#setCacheMode
	 * @see org.hibernate.query.NativeQuery#setCacheMode
	 */
	public static final String CACHE_MODE = "org.hibernate.cacheMode";

	/**
	 * The cache region to use.
	 *
	 * @see org.hibernate.query.Query#setCacheRegion
	 * @see org.hibernate.query.NativeQuery#setCacheRegion
	 */
	public static final String CACHE_REGION = "org.hibernate.cacheRegion";

	/**
	 * Are the query results cacheable?
	 *
	 * @see org.hibernate.query.Query#setCacheable
	 * @see org.hibernate.query.NativeQuery#setCacheable
	 */
	public static final String CACHEABLE = "org.hibernate.cacheable";

	/**
	 * Is the query callable?  Note: only valid for named native sql queries.
	 */
	public static final String CALLABLE = "org.hibernate.callable";

	/**
	 * Defines a comment to be applied to the SQL sent to the database.
	 *
	 * @see org.hibernate.query.Query#setComment
	 * @see org.hibernate.query.NativeQuery#setComment
	 */
	public static final String COMMENT = "org.hibernate.comment";

	/**
	 * Defines the JDBC fetch size to use.
	 *
	 * @see org.hibernate.query.Query#setFetchSize
	 * @see org.hibernate.query.NativeQuery#setFetchSize
	 */
	public static final String FETCH_SIZE = "org.hibernate.fetchSize";

	/**
	 * The flush mode to associate with the execution of the query.
	 *
	 * @see org.hibernate.query.Query#setHibernateFlushMode
	 * @see org.hibernate.query.NativeQuery#setHibernateFlushMode
	 * @see org.hibernate.Session#setHibernateFlushMode
	 */
	public static final String FLUSH_MODE = "org.hibernate.flushMode";

	/**
	 * Should entities returned from the query be set in read only mode?
	 *
	 * @see org.hibernate.query.Query#setReadOnly
	 * @see org.hibernate.query.NativeQuery#setReadOnly
	 * @see org.hibernate.Session#setReadOnly
	 */
	public static final String READ_ONLY = "org.hibernate.readOnly";

	/**
	 * Apply a Hibernate query timeout, which is defined in <b>seconds</b>.
	 *
	 * @see org.hibernate.query.Query#setTimeout
	 * @see org.hibernate.query.NativeQuery#setTimeout
	 */
	public static final String TIMEOUT_HIBERNATE = "org.hibernate.timeout";

	/**
	 * Apply a JPA query timeout, which is defined in <b>milliseconds</b>.
	 */
	public static final String TIMEOUT_JPA = "javax.persistence.query.timeout";

	/**
	 * Apply a JPA query timeout, which is defined in <b>milliseconds</b>.
	 */
	public static final String TIMEOUT_JAKARTA_JPA = "jakarta.persistence.query.timeout";

	/**
	 * Available to apply lock mode to a native SQL query since JPA requires that
	 * {@link jakarta.persistence.Query#setLockMode} throw an IllegalStateException if called for a native query.
	 * <p/>
	 * Accepts a {@link jakarta.persistence.LockModeType} or a {@link org.hibernate.LockMode}
	 */
	public static final String NATIVE_LOCKMODE = "org.hibernate.lockMode";

	/**
	 * Hint to enable/disable the follow-on-locking mechanism provided by {@link org.hibernate.dialect.Dialect#useFollowOnLocking(QueryParameters)}.
	 * A value of {@code true} enables follow-on-locking, whereas a value of {@code false} disables it.
	 * If the value is {@code null}, the {@code Dialect} strategy is going to be used instead.
	 *
	 * @since 5.2
	 */
	public static final String FOLLOW_ON_LOCKING = "hibernate.query.followOnLocking";

	/**
	 * Hint to enable/disable the pass-distinct-through mechanism.
	 * A value of {@code true} enables pass-distinct-through, whereas a value of {@code false} disables it.
	 * When the pass-distinct-through is disabled, the HQL and JPQL distinct clause is no longer passed to the SQL statement.
	 *
	 * @since 5.2
	 */
	public static final String PASS_DISTINCT_THROUGH = "hibernate.query.passDistinctThrough";

	/**
	 * Hint for specifying query spaces to be applied to a native (SQL) query.
	 *
	 * Passed value can be any of:<ul>
	 *     <li>List of the spaces</li>
	 *     <li>array of the spaces</li>
	 *     <li>String as "whitespace"-separated list of the spaces</li>
	 * </ul>
	 *
	 * Note that the passed space need not match to any real spaces/tables in
	 * the underlying query.  This can be used to completely circumvent
	 * the auto-flush checks as well as any cache invalidation that might
	 * occur as part of a flush.  See the documentation on SynchronizeableQuery
	 * for details.  See also {@link FlushMode#MANUAL}
	 *
	 * @see org.hibernate.SynchronizeableQuery
	 * @see #FLUSH_MODE
	 */
	public static final String NATIVE_SPACES = "org.hibernate.query.native.spaces";

}
