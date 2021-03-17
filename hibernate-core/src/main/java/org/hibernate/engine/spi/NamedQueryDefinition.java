/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;

/**
 * Definition of a named query, defined in the mapping metadata.  Additionally, as of JPA 2.1, named query definition
 * can also come from a compiled query.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class NamedQueryDefinition implements Serializable {
	private final String name;
	private final String query;
	private final boolean cacheable;
	private final String cacheRegion;
	private final Integer timeout;
	private final LockOptions lockOptions;
	private final Integer fetchSize;
	private final FlushMode flushMode;
	private final Map parameterTypes;
	private final CacheMode cacheMode;
	private final boolean readOnly;
	private final String comment;
	private final Boolean passDistinctThrough;

	// added for jpa 2.1
	private final Integer firstResult;
	private final Integer maxResults;

	/**
	 * This form is used to bind named queries from Hibernate metadata, both {@code hbm.xml} files and
	 * {@link org.hibernate.annotations.NamedQuery} annotation.
	 *
	 * @param name The name under which to key/register the query
	 * @param query The query string.
	 * @param cacheable Is the query cacheable?
	 * @param cacheRegion If cacheable, was there a specific region named?
	 * @param timeout Query timeout, {@code null} indicates no timeout
	 * @param fetchSize Fetch size associated with the query, {@code null} indicates no limit
	 * @param flushMode Flush mode associated with query
	 * @param cacheMode Cache mode associated with query
	 * @param readOnly Should entities returned from this query (those not already associated with the Session anyway)
	 * 		be loaded as read-only?
	 * @param comment SQL comment to be used in the generated SQL, {@code null} indicates none
	 * @param parameterTypes (no idea, afaict this is always passed as null)
	 *
	 * @deprecated Use {@link NamedQueryDefinitionBuilder} instead.
	 */
	@Deprecated
	public NamedQueryDefinition(
			String name,
			String query,
			boolean cacheable,
			String cacheRegion,
			Integer timeout,
			Integer fetchSize,
			FlushMode flushMode,
			CacheMode cacheMode,
			boolean readOnly,
			String comment,
			Map parameterTypes) {
		this(
				name,
				query,
				cacheable,
				cacheRegion,
				timeout,
				LockOptions.WAIT_FOREVER,
				fetchSize,
				flushMode,
				cacheMode,
				readOnly,
				comment,
				parameterTypes
		);
	}

	/**
	 * This version is used to bind named queries defined via {@link javax.persistence.NamedQuery}.
	 *
	 * @param name The name under which to key/register the query
	 * @param query The query string.
	 * @param cacheable Is the query cacheable?
	 * @param cacheRegion If cacheable, was there a specific region named?
	 * @param timeout Query timeout, {@code null} indicates no timeout
	 * @param lockTimeout Specifies the lock timeout for queries that apply lock modes.
	 * @param fetchSize Fetch size associated with the query, {@code null} indicates no limit
	 * @param flushMode Flush mode associated with query
	 * @param cacheMode Cache mode associated with query
	 * @param readOnly Should entities returned from this query (those not already associated with the Session anyway)
	 * 		be loaded as read-only?
	 * @param comment SQL comment to be used in the generated SQL, {@code null} indicates none
	 * @param parameterTypes (no idea, afaict this is always passed as null)
	 *
	 * @deprecated Use {@link NamedQueryDefinitionBuilder} instead.
	 */
	@Deprecated
	public NamedQueryDefinition(
			String name,
			String query,
			boolean cacheable,
			String cacheRegion,
			Integer timeout,
			Integer lockTimeout,
			Integer fetchSize,
			FlushMode flushMode,
			CacheMode cacheMode,
			boolean readOnly,
			String comment,
			Map parameterTypes) {
		this(
				name,
				query,
				cacheable,
				cacheRegion,
				timeout,
				new LockOptions().setTimeOut( lockTimeout ),
				fetchSize,
				flushMode,
				cacheMode,
				readOnly,
				comment,
				parameterTypes,
				null,		// firstResult
				null,		// maxResults
				null 		// passDistinctThrough
		);
	}

	NamedQueryDefinition(
			String name,
			String query,
			boolean cacheable,
			String cacheRegion,
			Integer timeout,
			LockOptions lockOptions,
			Integer fetchSize,
			FlushMode flushMode,
			CacheMode cacheMode,
			boolean readOnly,
			String comment,
			Map parameterTypes,
			Integer firstResult,
			Integer maxResults,
			Boolean passDistinctThrough) {
		this.name = name;
		this.query = query;
		this.cacheable = cacheable;
		this.cacheRegion = cacheRegion;
		this.timeout = timeout;
		this.lockOptions = lockOptions;
		this.fetchSize = fetchSize;
		this.flushMode = flushMode;
		this.parameterTypes = parameterTypes;
		this.cacheMode = cacheMode;
		this.readOnly = readOnly;
		this.comment = comment;

		this.firstResult = firstResult;
		this.maxResults = maxResults;
		this.passDistinctThrough = passDistinctThrough;
	}

	public String getName() {
		return name;
	}

	public String getQueryString() {
		return query;
	}

	public boolean isCacheable() {
		return cacheable;
	}

	public String getCacheRegion() {
		return cacheRegion;
	}

	public Integer getFetchSize() {
		return fetchSize;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public FlushMode getFlushMode() {
		return flushMode;
	}

	public Map getParameterTypes() {
		// todo : currently these are never used...
		return parameterTypes;
	}

	public String getQuery() {
		return query;
	}

	public CacheMode getCacheMode() {
		return cacheMode;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public String getComment() {
		return comment;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public Integer getFirstResult() {
		return firstResult;
	}

	public Integer getMaxResults() {
		return maxResults;
	}

	public Boolean getPassDistinctThrough() {
		return passDistinctThrough;
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + name + " [" + query + "])";
	}

	public NamedQueryDefinition makeCopy(String name) {
		return new NamedQueryDefinition(
				name,
				getQuery(),
				isCacheable(),
				getCacheRegion(),
				getTimeout(),
				getLockOptions(),
				getFetchSize(),
				getFlushMode(),
				getCacheMode(),
				isReadOnly(),
				getComment(),
				getParameterTypes(),
				getFirstResult(),
				getMaxResults(),
				getPassDistinctThrough()
		);
	}
}
