/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.CacheMode;
import org.hibernate.query.QueryFlushMode;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares a named query written in native SQL.
 * <p>
 * Whereas {@link jakarta.persistence.NamedNativeQuery} allows settings to be
 * specified using stringly-typed {@link jakarta.persistence.QueryHint}s, this
 * annotation is typesafe.
 * <p>
 * Note that the members of this annotation correspond to hints enumerated by
 * {@link org.hibernate.jpa.AvailableHints}.
 *
 * @author Emmanuel Bernard
 *
 * @see org.hibernate.query.NativeQuery
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Repeatable(NamedNativeQueries.class)
public @interface NamedNativeQuery {
	/**
	 * The name of this query. Must be unique within a persistence unit.
	 *
	 * @see org.hibernate.SessionFactory#addNamedQuery
	 * @see org.hibernate.Session#createNamedQuery
	 */
	String name();

	/**
	 * The text of the SQL query.
	 */
	String query();

	/**
	 * The resulting {@code Class}.
	 * <p>
	 * Should not be used in conjunction with {@link #resultSetMapping()}
	 */
	Class<?> resultClass() default void.class;

	/**
	 * The name of a {@link jakarta.persistence.SqlResultSetMapping}.
	 * <p>
	 * Should not be used in conjunction with {@link #resultClass()}.
	 */
	String resultSetMapping() default "";

	/**
	 * Determines whether the session should be flushed before
	 * executing the query.
	 *
	 * @see org.hibernate.query.CommonQueryContract#setQueryFlushMode(QueryFlushMode)
	 *
	 * @since 7.0
	 */
	QueryFlushMode flush() default QueryFlushMode.DEFAULT;

	/**
	 * The flush mode for the query.
	 *
	 * @see org.hibernate.query.CommonQueryContract#setFlushMode(jakarta.persistence.FlushModeType)
	 * @see org.hibernate.jpa.HibernateHints#HINT_FLUSH_MODE
	 *
	 * @deprecated use {@link #flush()}
	 */
	@Deprecated(since = "7", forRemoval = true)
	FlushModeType flushMode() default FlushModeType.PERSISTENCE_CONTEXT;

	/**
	 * Whether the query results are cacheable.
	 * Default is {@code false}, that is, not cacheable.
	 *
	 * @see org.hibernate.query.SelectionQuery#setCacheable(boolean)
	 * @see org.hibernate.jpa.HibernateHints#HINT_CACHEABLE
	 */
	boolean cacheable() default false;

	/**
	 * If the query results are cacheable, the name of the query cache region.
	 *
	 * @see org.hibernate.query.SelectionQuery#setCacheRegion(String)
	 * @see org.hibernate.jpa.HibernateHints#HINT_CACHE_REGION
	 */
	String cacheRegion() default "";

	/**
	 * The number of rows fetched by the JDBC driver per trip.
	 *
	 * @see org.hibernate.query.SelectionQuery#setFetchSize(int)
	 * @see org.hibernate.jpa.HibernateHints#HINT_FETCH_SIZE
	 */
	int fetchSize() default -1;

	/**
	 * The query timeout in seconds.
	 * Default is no timeout.
	 *
	 * @see org.hibernate.query.CommonQueryContract#setTimeout(int)
	 * @see org.hibernate.jpa.HibernateHints#HINT_TIMEOUT
	 * @see org.hibernate.jpa.SpecHints#HINT_SPEC_QUERY_TIMEOUT
	 */
	int timeout() default -1;

	/**
	 * A comment added to the SQL query.
	 * Useful when engaging with DBA.
	 *
	 * @see org.hibernate.query.CommonQueryContract#setComment(String)
	 * @see org.hibernate.jpa.HibernateHints#HINT_COMMENT
	 */
	String comment() default "";

	/**
	 * The cache storage mode for objects returned by this query.
	 *
	 * @see org.hibernate.query.Query#setCacheMode(CacheMode)
	 * @see org.hibernate.jpa.SpecHints#HINT_SPEC_CACHE_STORE_MODE
	 *
	 * @since 6.2
	 */
	CacheStoreMode cacheStoreMode() default CacheStoreMode.USE;

	/**
	 * The cache retrieval mode for objects returned by  this query.
	 *
	 * @see org.hibernate.query.Query#setCacheMode(CacheMode)
	 * @see org.hibernate.jpa.SpecHints#HINT_SPEC_CACHE_RETRIEVE_MODE
	 *
	 * @since 6.2
	 */
	CacheRetrieveMode cacheRetrieveMode() default CacheRetrieveMode.USE;

	/**
	 * The cache interaction mode for this query.
	 *
	 * @see org.hibernate.query.SelectionQuery#setCacheMode(CacheMode)
	 * @see org.hibernate.jpa.HibernateHints#HINT_CACHE_MODE
	 */
	CacheMode cacheMode() default CacheMode.NORMAL;

	/**
	 * Whether the results should be loaded in read-only mode.
	 * Default is {@code false}.
	 *
	 * @see org.hibernate.query.SelectionQuery#setReadOnly(boolean)
	 * @see org.hibernate.jpa.HibernateHints#HINT_READ_ONLY
	 */
	boolean readOnly() default false;

	/**
	 * The {@linkplain org.hibernate.query.SynchronizeableQuery query spaces}
	 * involved in this query.
	 * <p>
	 * Typically, the names of tables which are referenced by the query.
	 *
	 * @see org.hibernate.query.SynchronizeableQuery#addSynchronizedQuerySpace
	 * @see org.hibernate.jpa.HibernateHints#HINT_NATIVE_SPACES
	 * @see Synchronize
	 */
	String[] querySpaces() default {};
}
