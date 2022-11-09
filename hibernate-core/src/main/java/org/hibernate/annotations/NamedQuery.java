/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.CacheMode;
import org.hibernate.Remove;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares a named query written in HQL or JPQL.
 * <p>
 * Extends {@link jakarta.persistence.NamedQuery} with additional settings.
 *
 * @author Carlos Gonzalez-Cadenas
 *
 * @see org.hibernate.query.Query
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Repeatable(NamedQueries.class)
public @interface NamedQuery {

	/**
	 * The name of this query.
	 */
	String name();

	/**
	 * The text of the HQL query.
	 */
	String query();

	/**
	 * The flush mode for this query.
	 *
	 * @see org.hibernate.query.CommonQueryContract#setFlushMode(jakarta.persistence.FlushModeType)
	 */
	FlushModeType flushMode() default FlushModeType.PERSISTENCE_CONTEXT;

	/**
	 * Whether the query results are cacheable.
	 * Default is {@code false}, that is, not cacheable.
	 *
	 * @see org.hibernate.query.SelectionQuery#setCacheable(boolean)
	 */
	boolean cacheable() default false;

	/**
	 * If the query results are cacheable, the name of the query cache region.
	 *
	 * @see org.hibernate.query.SelectionQuery#setCacheRegion(String)
	 */
	String cacheRegion() default "";

	/**
	 * The number of rows fetched by the JDBC driver per trip.
	 *
	 * @see org.hibernate.query.SelectionQuery#setFetchSize(int)
	 */
	int fetchSize() default -1;

	/**
	 * The query timeout in seconds.
	 * Default is no timeout.
	 *
	 * @see org.hibernate.query.CommonQueryContract#setTimeout(int)
	 */
	int timeout() default -1;

	/**
	 * A comment added to the generated SQL query.
	 * Useful when engaging with DBA.
	 *
	 * @see org.hibernate.query.CommonQueryContract#setComment(String)
	 */
	String comment() default "";

	/**
	 * The cache storage mode for objects returned by this query.
	 *
	 * @see org.hibernate.query.Query#setCacheMode(CacheMode)
	 */
	CacheStoreMode cacheStoreMode() default CacheStoreMode.USE;

	/**
	 * The cache retrieval mode for objects returned by this query.
	 *
	 * @see org.hibernate.query.Query#setCacheMode(CacheMode)
	 */
	CacheRetrieveMode cacheRetrieveMode() default CacheRetrieveMode.USE;

	/**
	 * The cache interaction mode for this query.
	 *
	 * @deprecated use {@link #cacheStoreMode()} and
	 *            {@link #cacheRetrieveMode()} since
	 *            {@link CacheModeType} is deprecated
	 */
	@Deprecated(since = "6.2") @Remove
	//TODO: actually, we won't remove it, we'll change its
	//      type to CacheMode and then un-deprecate it
	CacheModeType cacheMode() default CacheModeType.NORMAL;

	/**
	 * Whether the results should be read-only.
	 * Default is {@code false}.
	 *
	 * @see org.hibernate.query.SelectionQuery#setReadOnly(boolean)
	 */
	boolean readOnly() default false;
}
