/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.FlushMode;
import org.hibernate.NativeQuery;
import org.hibernate.Query;

/**
 * List of hints that may be passed to {@link jakarta.persistence.Query#setHint(String, Object)}
 * to control execution of a query. Each of these hints corresponds to a typesafe operation of
 * the {@link Query} interface, and so hints are only necessary for programs
 * working with the JPA APIs.
 *
 * @see org.hibernate.jpa.QueryHints
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
	 * @see Query#setCacheMode
	 * @see NativeQuery#setCacheMode
	 */
	public static final String CACHE_MODE = "org.hibernate.cacheMode";

	/**
	 * The cache region to use.
	 *
	 * @see Query#setCacheRegion
	 * @see NativeQuery#setCacheRegion
	 */
	public static final String CACHE_REGION = "org.hibernate.cacheRegion";

	/**
	 * Are the query results cacheable?
	 *
	 * @see Query#setCacheable
	 * @see NativeQuery#setCacheable
	 */
	public static final String CACHEABLE = "org.hibernate.cacheable";

	/**
	 * Is the named stored procedure a function?
	 */
	public static final String CALLABLE_FUNCTION = "org.hibernate.callableFunction";

	/**
	 * Defines a comment to be applied to the SQL sent to the database.
	 *
	 * @see Query#setComment
	 * @see NativeQuery#setComment
	 */
	public static final String COMMENT = "org.hibernate.comment";

	/**
	 * Defines the JDBC fetch size to use.
	 *
	 * @see Query#setFetchSize
	 * @see NativeQuery#setFetchSize
	 */
	public static final String FETCH_SIZE = "org.hibernate.fetchSize";

	/**
	 * The flush mode to associate with the execution of the query.
	 *
	 * @see Query#setHibernateFlushMode
	 * @see NativeQuery#setHibernateFlushMode
	 * @see org.hibernate.Session#setHibernateFlushMode
	 */
	public static final String FLUSH_MODE = "org.hibernate.flushMode";

	/**
	 * Should entities returned from the query be set in read only mode?
	 *
	 * @see Query#setReadOnly
	 * @see NativeQuery#setReadOnly
	 * @see org.hibernate.Session#setReadOnly
	 */
	public static final String READ_ONLY = "org.hibernate.readOnly";

	/**
	 * Apply a Hibernate query timeout, which is defined in <b>seconds</b>.
	 *
	 * @see Query#setTimeout
	 * @see NativeQuery#setTimeout
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
	 * Apply lock mode to a native SQL query since JPA requires that
	 * {@link jakarta.persistence.Query#setLockMode} throw an {@code IllegalStateException}
	 * if called for a native query.
	 * <p/>
	 * Accepts a {@link jakarta.persistence.LockModeType} or a {@link org.hibernate.LockMode}
	 */
	public static final String NATIVE_LOCKMODE = "org.hibernate.lockMode";

	/**
	 * Hint to enable/disable the follow-on-locking mechanism provided by
	 * {@link org.hibernate.dialect.Dialect#useFollowOnLocking(String, org.hibernate.query.spi.QueryOptions)}.
	 * A value of {@code true} enables follow-on-locking, whereas a value of
	 * {@code false} disables it. If the value is {@code null}, the
	 * {@code Dialect}'s default strategy is used.
	 *
	 * @since 5.2
	 */
	public static final String FOLLOW_ON_LOCKING = "hibernate.query.followOnLocking";

	/**
	 * Hint for specifying query spaces to be applied to a native (SQL) query.
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
	 * @see #FLUSH_MODE
	 */
	public static final String NATIVE_SPACES = "org.hibernate.query.native.spaces";

}
