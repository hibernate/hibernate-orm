/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa;

import jakarta.persistence.EntityManagerFactory;

/**
 * List of Hibernate-specific (extension) hints available to query,
 * load, and lock scenarios.
 * <p>
 * Some hints are only effective in certain scenarios, which is noted 
 * on each constant's documentation.
 *
 * @author Steve Ebersole
 */
public interface HibernateHints {
	/**
	 * Hint for specifying the {@link org.hibernate.FlushMode} to 
	 * apply to an {@link jakarta.persistence.EntityManager} or 
	 * {@link jakarta.persistence.Query}.
	 *
	 * @see org.hibernate.query.Query#setHibernateFlushMode
	 * @see org.hibernate.Session#setHibernateFlushMode
	 */
	String HINT_FLUSH_MODE = "org.hibernate.flushMode";

	/**
	 * Hint for specifying a
	 * {@linkplain org.hibernate.query.CommonQueryContract#setTimeout
	 * query timeout}, in seconds.
	 *
	 * @see org.hibernate.query.CommonQueryContract#setTimeout
	 * @see java.sql.Statement#setQueryTimeout
	 * @see SpecHints#HINT_SPEC_QUERY_TIMEOUT
	 */
	String HINT_TIMEOUT = "org.hibernate.timeout";

	/**
	 * Hint for specifying that objects loaded into the persistence
	 * context as a result of a query should be associated with the
	 * persistence context in
	 * {@linkplain org.hibernate.query.SelectionQuery#setReadOnly
	 * read-only mode}.
	 *
	 * @see org.hibernate.query.SelectionQuery#setReadOnly
	 * @see org.hibernate.Session#setDefaultReadOnly
	 */
	String HINT_READ_ONLY = "org.hibernate.readOnly";

	/**
	 * Hint for specifying a JDBC fetch size to be applied to the
	 * statement.
	 *
	 * @see org.hibernate.query.SelectionQuery#setFetchSize
	 * @see java.sql.Statement#setFetchSize
	 */
	String HINT_FETCH_SIZE = "org.hibernate.fetchSize";

	/**
	 * Hint for specifying whether results from a query should be 
	 * stored in the query cache.
	 *
	 * @see org.hibernate.query.SelectionQuery#setCacheable
	 */
	String HINT_CACHEABLE = "org.hibernate.cacheable";

	/**
	 * Hint for specifying the region of the query cache into which
	 * the results should be stored.
	 *
	 * @implSpec No effect unless {@link #HINT_CACHEABLE} is set to {@code true}
	 *
	 * @see org.hibernate.query.SelectionQuery#setCacheRegion
	 */
	String HINT_CACHE_REGION = "org.hibernate.cacheRegion";

	/**
	 * Hint for specifying the {@link org.hibernate.CacheMode} to use.
	 *
	 * @implSpec No effect unless {@link #HINT_CACHEABLE} is set to {@code true}
	 *
	 * @see org.hibernate.query.SelectionQuery#setCacheMode
	 */
	String HINT_CACHE_MODE = "org.hibernate.cacheMode";

	/**
	 * Hint for specifying a database comment to be appended to the 
	 * SQL statement sent to the database.
	 *
	 * @implSpec Not valid for {@link org.hibernate.procedure.ProcedureCall},
	 *           nor for {@link jakarta.persistence.StoredProcedureQuery}.
	 *
	 * @see org.hibernate.query.SelectionQuery#setComment
	 */
	String HINT_COMMENT = "org.hibernate.comment";

	/**
	 * Hint to enable or disable the follow-on locking mechanism provided
	 * by {@link org.hibernate.dialect.Dialect#useFollowOnLocking}.
	 * <p>
	 * A value of {@code true} enables follow-on-locking, whereas a value
	 * of {@code false} disables it. If the value is {@code null}, the
	 * dialect itself will determine whether follow-on locking is used.
	 *
	 * @see org.hibernate.LockOptions#setFollowOnLocking(Boolean)
	 *
	 * @since 5.2
	 */
	String HINT_FOLLOW_ON_LOCKING = "hibernate.query.followOnLocking";

	/**
	 * Hint for specifying the lock mode to apply to the results of a
	 * native query.
	 * <p>
	 * Accepts either a {@link jakarta.persistence.LockModeType} or a
	 * {@link org.hibernate.LockMode}.
	 * 
	 * @apiNote While Hibernate supports applying a lock mode to a 
	 *          native query, the JPA specification requires that 
	 *          {@link jakarta.persistence.Query#setLockMode} throw 
	 *          an {@link IllegalStateException} in this scenario.
	 */
	String HINT_NATIVE_LOCK_MODE = "org.hibernate.lockMode";

	/**
	 * Hint for specifying the 
	 * {@linkplain org.hibernate.query.SynchronizeableQuery query spaces}
	 * that affect the results of a native query.
	 * <p>
	 * Passed value can be any of:
	 * <ul>
	 *     <li>a {@link org.hibernate.mapping.List} of the spaces,
	 *     <li>an array of the spaces, or
	 *     <li>a string with a whitespace-separated list of the spaces.
	 * </ul>
	 * <p>
	 * Typically, these are the names of tables which are referenced by
	 * the query.
	 *
	 * @see org.hibernate.query.SynchronizeableQuery#addSynchronizedQuerySpace 
	 * @see #HINT_FLUSH_MODE
	 * @see org.hibernate.annotations.NamedNativeQuery#querySpaces
	 */
	String HINT_NATIVE_SPACES = "org.hibernate.query.native.spaces";

	/**
	 * Whether to treat a {@link org.hibernate.procedure.ProcedureCall}
	 * or {@link jakarta.persistence.StoredProcedureQuery} as a call
	 * to a function rather than a call to a procedure.
	 */
	String HINT_CALLABLE_FUNCTION = "org.hibernate.callableFunction";

	/**
	 * Hint for specifying the tenant-id to use when creating an
	 * {@linkplain EntityManagerFactory#createEntityManager(java.util.Map) EntityManager}.
	 *
	 * @see org.hibernate.SessionBuilder#tenantIdentifier
	 */
	String HINT_TENANT_ID = "org.hibernate.tenantId";
}
