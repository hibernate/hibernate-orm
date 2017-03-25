/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.query.CommonQueryContract;
import org.hibernate.type.Type;

/**
 * Defines the aspects of query definition that apply to all forms of querying.
 *
 * @author Steve Ebersole
 *
 * @deprecated (since 5.2) use {@link CommonQueryContract} instead.
 */
@Deprecated
public interface BasicQueryContract {
	/**
	 * (Re)set the current FlushMode in effect for this query.
	 *
	 * @param flushMode The new FlushMode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getHibernateFlushMode()
	 *
	 * @deprecated (since 5.2) use {@link #setHibernateFlushMode} instead
	 */
	@Deprecated
	default CommonQueryContract setFlushMode(FlushMode flushMode) {
		setHibernateFlushMode( flushMode );
		return (CommonQueryContract) this;
	}

	/**
	 * Obtain the FlushMode in effect for this query.  By default, the query inherits the FlushMode of the Session
	 * from which it originates.
	 *
	 * @return The query FlushMode.
	 *
	 * @see Session#getFlushMode()
	 * @see FlushMode
	 */
	FlushMode getHibernateFlushMode();

	/**
	 * (Re)set the current FlushMode in effect for this query.
	 *
	 * @param flushMode The new FlushMode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getHibernateFlushMode()
	 */
	CommonQueryContract setHibernateFlushMode(FlushMode flushMode);

	/**
	 * Obtain the CacheMode in effect for this query.  By default, the query inherits the CacheMode of the Session
	 * from which is originates.
	 *
	 * NOTE: The CacheMode here only effects reading/writing of the query cache, not the
	 * entity/collection caches.
	 *
	 * @return The query CacheMode.
	 *
	 * @see Session#getCacheMode()
	 * @see CacheMode
	 */
	CacheMode getCacheMode();

	/**
	 * (Re)set the current CacheMode in effect for this query.
	 *
	 * @param cacheMode The new CacheMode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getCacheMode()
	 */
	CommonQueryContract setCacheMode(CacheMode cacheMode);

	/**
	 * Are the results of this query eligible for second level query caching?  This is different that second level
	 * caching of any returned entities and collections.
	 *
	 * NOTE: the query being "eligible" for caching does not necessarily mean its results will be cached.  Second level
	 * query caching still has to be enabled on the {@link SessionFactory} for this to happen.  Usually that is
	 * controlled by the {@code hibernate.cache.use_query_cache} configuration setting.
	 *
	 * @return {@code true} if the query results are eligible for caching, {@code false} otherwise.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_QUERY_CACHE
	 */
	boolean isCacheable();

	/**
	 * Enable/disable second level query (result) caching for this query.
	 *
	 * @param cacheable Should the query results be cacheable?
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #isCacheable
	 */
	CommonQueryContract setCacheable(boolean cacheable);

	/**
	 * Obtain the name of the second level query cache region in which query results will be stored (if they are
	 * cached, see the discussion on {@link #isCacheable()} for more information).  {@code null} indicates that the
	 * default region should be used.
	 *
	 * @return The specified cache region name into which query results should be placed; {@code null} indicates
	 * the default region.
	 */
	String getCacheRegion();

	/**
	 * Set the name of the cache region where query results should be cached (if cached at all).
	 *
	 * @param cacheRegion the name of a query cache region, or {@code null} to indicate that the default region
	 * should be used.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getCacheRegion()
	 */
	CommonQueryContract setCacheRegion(String cacheRegion);

	/**
	 * Obtain the query timeout <b>in seconds</b>.  This value is eventually passed along to the JDBC query via
	 * {@link java.sql.Statement#setQueryTimeout(int)}.  Zero indicates no timeout.
	 *
	 * @return The timeout <b>in seconds</b>
	 *
	 * @see java.sql.Statement#getQueryTimeout()
	 * @see java.sql.Statement#setQueryTimeout(int)
	 */
	Integer getTimeout();

	/**
	 * Set the query timeout <b>in seconds</b>.
	 *
	 * NOTE it is important to understand that any value set here is eventually passed directly through to the JDBC
	 * Statement which expressly disallows negative values.  So negative values should be avoided as a general rule.
	 *
	 * @param timeout the timeout <b>in seconds</b>
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getTimeout()
	 */
	CommonQueryContract setTimeout(int timeout);

	/**
	 * Obtain the JDBC fetch size hint in effect for this query.  This value is eventually passed along to the JDBC
	 * query via {@link java.sql.Statement#setFetchSize(int)}.  As defined b y JDBC, this value is a hint to the
	 * driver to indicate how many rows to fetch from the database when more rows are needed.
	 *
	 * NOTE : JDBC expressly defines this value as a hint.  It may or may not have any effect on the actual
	 * query execution and ResultSet processing depending on the driver.
	 *
	 * @return The timeout <b>in seconds</b>
	 *
	 * @see java.sql.Statement#getFetchSize()
	 * @see java.sql.Statement#setFetchSize(int)
	 */
	Integer getFetchSize();

	/**
	 * Sets a JDBC fetch size hint for the query.
	 *
	 * @param fetchSize the fetch size hint
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getFetchSize()
	 */
	CommonQueryContract setFetchSize(int fetchSize);

	/**
	 * Should entities and proxies loaded by this Query be put in read-only mode? If the
	 * read-only/modifiable setting was not initialized, then the default
	 * read-only/modifiable setting for the persistence context is returned instead.
	 *
	 * @see #setReadOnly(boolean)
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * The read-only/modifiable setting has no impact on entities/proxies returned by the
	 * query that existed in the session beforeQuery the query was executed.
	 *
	 * @return {@code true} if the entities and proxies loaded by the query will be put
	 * in read-only mode; {@code false} otherwise (they will be modifiable)
	 */
	boolean isReadOnly();

	/**
	 * Set the read-only/modifiable mode for entities and proxies
	 * loaded by this Query. This setting overrides the default setting
	 * for the persistence context.
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * To set the default read-only/modifiable setting used for
	 * entities and proxies that are loaded into the session:
	 * @see org.hibernate.engine.spi.PersistenceContext#setDefaultReadOnly(boolean)
	 * @see Session#setDefaultReadOnly(boolean)
	 *
	 * Read-only entities are not dirty-checked and snapshots of persistent
	 * state are not maintained. Read-only entities can be modified, but
	 * changes are not persisted.
	 *
	 * When a proxy is initialized, the loaded entity will have the same
	 * read-only/modifiable setting as the uninitialized
	 * proxy has, regardless of the session's current setting.
	 *
	 * The read-only/modifiable setting has no impact on entities/proxies
	 * returned by the query that existed in the session beforeQuery the query was executed.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @param readOnly {@code true} indicates that entities and proxies loaded by the query
	 * are to be put in read-only mode; {@code false} indicates that entities and proxies
	 * loaded by the query will be put in modifiable mode
	 */
	CommonQueryContract setReadOnly(boolean readOnly);

	/**
	 * Return the Hibernate types of the query results.
	 *
	 * @return an array of types
	 *
	 * @deprecated (since 5.2) with no replacement; to be removed in 6.0
	 */
	@Deprecated
	Type[] getReturnTypes();
}
