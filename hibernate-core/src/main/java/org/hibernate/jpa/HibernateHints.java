/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa;

import org.hibernate.Locking;

/**
 * List of Hibernate-specific (extension) hints available to query,
 * load, and lock scenarios.
 * <p>
 * Some hints are only effective in certain scenarios, which is noted
 * on each constant's documentation.
 *
 * @apiNote The stringly-typed hints defined here correspond to
 *          typesafe operations of Hibernate's native APIs, and
 *          should not be used unless portability between JPA
 *          implementations is of very great importance.
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
	 * Hint to enable or disable the follow-on locking.<ul>
	 *     <li>{@code false} - {@linkplain Locking.FollowOn#DISALLOW disallows} follow-on locking
	 *     <li>{@code null} or {@code true} - {@linkplain Locking.FollowOn#ALLOW allows} follow-on locking
	 * </ul>
	 *
	 * @see org.hibernate.LockOptions#setFollowOnLocking(Boolean)
	 *
	 * @since 5.2
	 *
	 * @deprecated Use {@linkplain #HINT_FOLLOW_ON_STRATEGY} instead to allow an additional option
	 * to {@linkplain Locking.FollowOn#IGNORE ignore} follow-on locking which will potentially
	 * skip locking some rows but may be useful for applications targeting multiple databases.
	 *
	 */
	@Deprecated(since = "7.1")
	String HINT_FOLLOW_ON_LOCKING = "hibernate.query.followOnLocking";

	/**
	 * Hint to indicate how follow-on locking should be handled.
	 * See {@linkplain Locking.FollowOn} for a discussion of the
	 * options.
	 *
	 * @since 7.1
	 */
	String HINT_FOLLOW_ON_STRATEGY = "hibernate.query.followOnStrategy";

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
	 * to a function rather than a call to a procedure. Set hint to
	 * {@link Boolean#TRUE TRUE} or {@code "true"} to indicated that
	 * the call should be treated as a function call.
	 * <p>
	 * When no other return type is indicated, a function is assumed
	 * to return {@link java.sql.Types#REF_CURSOR REF_CURSOR}.
	 *
	 * @see org.hibernate.procedure.ProcedureCall#markAsFunctionCall
	 * @see #HINT_CALLABLE_FUNCTION_RETURN_TYPE
	 */
	String HINT_CALLABLE_FUNCTION = "org.hibernate.callableFunction";

	/**
	 * The {@linkplain org.hibernate.type.SqlTypes JDBC type code},
	 * {@linkplain jakarta.persistence.metamodel.Type type}, or
	 * {@link Class} of the value returned by a SQL function called
	 * via {@link org.hibernate.procedure.ProcedureCall} or
	 * {@link jakarta.persistence.StoredProcedureQuery}. Has the side
	 * effect of causing the call to be treated as a function call
	 * rather than a call to a stored procedure.
	 *
	 * @see org.hibernate.procedure.ProcedureCall#markAsFunctionCall(int)
	 * @see org.hibernate.procedure.ProcedureCall#markAsFunctionCall(jakarta.persistence.metamodel.Type)
	 * @see org.hibernate.procedure.ProcedureCall#markAsFunctionCall(Class)
	 */
	String HINT_CALLABLE_FUNCTION_RETURN_TYPE = "hibernate.procedure.function_return_jdbc_type_code";

	/**
	 * Hint for specifying the tenant id to use when creating an
	 * {@link jakarta.persistence.EntityManagerFactory#createEntityManager(java.util.Map) EntityManager}.
	 *
	 * @see org.hibernate.SessionBuilder#tenantIdentifier
	 * @see jakarta.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
	 */
	String HINT_TENANT_ID = "org.hibernate.tenantId";

	/**
	 * Hint to enable a fetch profile for a given
	 * {@link jakarta.persistence.EntityManager#setProperty(String, Object) EntityManager}
	 * or {@link jakarta.persistence.Query#setHint(String, Object) Query}.
	 *
	 * @see org.hibernate.Session#enableFetchProfile(String)
	 * @see org.hibernate.query.SelectionQuery#enableFetchProfile(String)
	 * @see jakarta.persistence.EntityManager#setProperty(String, Object)
	 * @see jakarta.persistence.Query#setHint(String, Object)
	 */
	String HINT_FETCH_PROFILE = "org.hibernate.fetchProfile";

	/**
	 * Hint to enable subselect fetching for a given
	 * {@link jakarta.persistence.EntityManager#setProperty(String, Object) EntityManager}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_SUBSELECT_FETCH
	 * @see jakarta.persistence.EntityManager#setProperty(String, Object)
	 */
	String HINT_ENABLE_SUBSELECT_FETCH = "org.hibernate.enableSubselectFetch";

	/**
	 * Hint to set the batch size for batch fetching for a given
	 * {@link jakarta.persistence.EntityManager#setProperty(String, Object) EntityManager}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_BATCH_FETCH_SIZE
	 * @see jakarta.persistence.EntityManager#setProperty(String, Object)
	 */
	String HINT_BATCH_FETCH_SIZE = "org.hibernate.batchFetchSize";

	/**
	 * Hint to set the batch size for JDBC batching for a given
	 * {@link jakarta.persistence.EntityManager#setProperty(String, Object) EntityManager}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#STATEMENT_BATCH_SIZE
	 * @see jakarta.persistence.EntityManager#setProperty(String, Object)
	 */
	String HINT_JDBC_BATCH_SIZE = "org.hibernate.jdbcBatchSize";

	/**
	 * Hint to enable or disable the query plan caching.
	 * <p>
	 * By default, query plan caching is enabled for HQL queries
	 * and immutable criteria queries i.e. created with {@link org.hibernate.cfg.AvailableSettings#CRITERIA_COPY_TREE}.
	 * Query plan caching can be disabled for any query by setting this property to {@code false}.
	 * Query plan caching can be enabled for mutable criteria queries by setting this property to {@code true}.
	 * <p>
	 * Setting this property to {@code true} for mutable criteria queries can lead to cache trashing,
	 * because the query plan is cached based on a copy of the criteria query.
	 * This is mostly useful when the same {@link org.hibernate.query.Query} should be executed multiple times,
	 * but with different parameter values to avoid re-translation of the criteria query.
	 * <p>
	 * Note that setting this property to {@code true} does not override the basic safety measures of Hibernate.
	 * Hibernate will never cache query plans that are not safe to cache, regardless of the value of this property.
	 *
	 * @see org.hibernate.query.SelectionQuery#setQueryPlanCacheable
	 *
	 * @since 6.3
	 */
	String HINT_QUERY_PLAN_CACHEABLE = "hibernate.query.plan.cacheable";

	/**
	 * Hint for specifying a query hint to be appended to the
	 * SQL statement sent to the database.
	 *
	 * @see org.hibernate.query.spi.MutableQueryOptions#addDatabaseHint(String)
	 * @see org.hibernate.query.Query#addQueryHint
	 * @since 6.5
	 */
	String HINT_QUERY_DATABASE = "hibernate.query.database";
}
