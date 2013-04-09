/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate;

import org.hibernate.type.Type;

/**
 * Defines the aspects of query definition that apply to all forms of querying.
 *
 * @author Steve Ebersole
 */
public interface BasicQueryContract {
	/**
	 * Obtain the FlushMode in effect for this query.  By default, the query inherits the FlushMode of the Session
	 * from which is originates.
	 *
	 * @return The query FlushMode.
	 *
	 * @see Session#getFlushMode()
	 * @see FlushMode
	 */
	public FlushMode getFlushMode();

	/**
	 * (Re)set the current FlushMode in effect for this query.
	 *
	 * @param flushMode The new FlushMode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getFlushMode()
	 */
	public BasicQueryContract setFlushMode(FlushMode flushMode);

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
	public CacheMode getCacheMode();

	/**
	 * (Re)set the current CacheMode in effect for this query.
	 *
	 * @param cacheMode The new CacheMode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getCacheMode()
	 */
	public BasicQueryContract setCacheMode(CacheMode cacheMode);

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
	public boolean isCacheable();

	/**
	 * Enable/disable second level query (result) caching for this query.
	 *
	 * @param cacheable Should the query results be cacheable?
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #isCacheable
	 */
	public BasicQueryContract setCacheable(boolean cacheable);

	/**
	 * Obtain the name of the second level query cache region in which query results will be stored (if they are
	 * cached, see the discussion on {@link #isCacheable()} for more information).  {@code null} indicates that the
	 * default region should be used.
	 *
	 * @return The specified cache region name into which query results should be placed; {@code null} indicates
	 * the default region.
	 */
	public String getCacheRegion();

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
	public BasicQueryContract setCacheRegion(String cacheRegion);

	/**
	 * Obtain the query timeout <b>in seconds</b>.  This value is eventually passed along to the JDBC query via
	 * {@link java.sql.Statement#setQueryTimeout(int)}.  Zero indicates no timeout.
	 *
	 * @return The timeout <b>in seconds</b>
	 *
	 * @see java.sql.Statement#getQueryTimeout()
	 * @see java.sql.Statement#setQueryTimeout(int)
	 */
	public Integer getTimeout();

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
	public BasicQueryContract setTimeout(int timeout);

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
	public Integer getFetchSize();

	/**
	 * Sets a JDBC fetch size hint for the query.
	 *
	 * @param fetchSize the fetch size hint
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getFetchSize()
	 */
	public BasicQueryContract setFetchSize(int fetchSize);

	/**
	 * Should entities and proxies loaded by this Query be put in read-only mode? If the
	 * read-only/modifiable setting was not initialized, then the default
	 * read-only/modifiable setting for the persistence context is returned instead.
	 * @see Query#setReadOnly(boolean)
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * The read-only/modifiable setting has no impact on entities/proxies returned by the
	 * query that existed in the session before the query was executed.
	 *
	 * @return true, entities and proxies loaded by the query will be put in read-only mode
	 *         false, entities and proxies loaded by the query will be put in modifiable mode
	 */
	public boolean isReadOnly();

	/**
	 * Set the read-only/modifiable mode for entities and proxies
	 * loaded by this Query. This setting overrides the default setting
	 * for the persistence context.
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * To set the default read-only/modifiable setting used for
	 * entities and proxies that are loaded into the session:
	 * @see org.hibernate.engine.spi.PersistenceContext#setDefaultReadOnly(boolean)
	 * @see org.hibernate.Session#setDefaultReadOnly(boolean)
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
	 * returned by the query that existed in the session before the query was executed.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @param readOnly true, entities and proxies loaded by the query will be put in read-only mode
	 *                 false, entities and proxies loaded by the query will be put in modifiable mode
	 */
	public BasicQueryContract setReadOnly(boolean readOnly);

	/**
	 * Return the Hibernate types of the query results.
	 *
	 * @return an array of types
	 */
	public Type[] getReturnTypes();
}
