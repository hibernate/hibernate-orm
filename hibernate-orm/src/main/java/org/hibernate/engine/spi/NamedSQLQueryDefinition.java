/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;

/**
 * Definition of a named native SQL query, defined in the mapping metadata.
 * 
 * @author Max Andersen
 * @author Steve Ebersole
 */
public class NamedSQLQueryDefinition extends NamedQueryDefinition {

	private NativeSQLQueryReturn[] queryReturns;
	private final List<String> querySpaces;
	private final boolean callable;
	private String resultSetRef;

	/**
	 * This form was initially used to construct a NamedSQLQueryDefinition from the binder code when a the
	 * result-set mapping information is not explicitly  provided in the query definition
	 * (i.e., no resultset-mapping used).
	 *
	 * @param name The name of named query
	 * @param query The sql query string
	 * @param queryReturns The in-lined query return definitions
	 * @param querySpaces Any specified query spaces (used for auto-flushing)
	 * @param cacheable Whether the query results are cacheable
	 * @param cacheRegion If cacheable, the region into which to store the results
	 * @param timeout A JDBC-level timeout to be applied
	 * @param fetchSize A JDBC-level fetch-size to be applied
	 * @param flushMode The flush mode to use for this query
	 * @param cacheMode The cache mode to use during execution and subsequent result loading
	 * @param readOnly Whether returned entities should be marked as read-only in the session
	 * @param comment Any sql comment to be applied to the query
	 * @param parameterTypes parameter type map
	 * @param callable Does the query string represent a callable object (i.e., proc)
	 *
	 * @deprecated Use {@link NamedSQLQueryDefinitionBuilder} instead.
	 */
	@Deprecated
	public NamedSQLQueryDefinition(
			String name,
			String query,
			NativeSQLQueryReturn[] queryReturns,
			List<String> querySpaces,
			boolean cacheable,
			String cacheRegion,
			Integer timeout,
			Integer fetchSize,
			FlushMode flushMode,
			CacheMode cacheMode,
			boolean readOnly,
			String comment,
			Map parameterTypes,
			boolean callable) {
		this(
				name,
				query,
				cacheable,
				cacheRegion,
				timeout,
				fetchSize,
				flushMode,
				cacheMode,
				readOnly,
				comment,
				parameterTypes,
				null,		// firstResult
				null,		// maxResults
				null, 		// resultSetRef
				querySpaces,
				callable,
				queryReturns
		);
	}

	/**
	 * This form was initially used to construct a NamedSQLQueryDefinition from the binder code when a
	 * resultset-mapping reference is used.
	 *
	 * @param name The name of named query
	 * @param query The sql query string
	 * @param resultSetRef The resultset-mapping name
	 * @param querySpaces Any specified query spaces (used for auto-flushing)
	 * @param cacheable Whether the query results are cacheable
	 * @param cacheRegion If cacheable, the region into which to store the results
	 * @param timeout A JDBC-level timeout to be applied
	 * @param fetchSize A JDBC-level fetch-size to be applied
	 * @param flushMode The flush mode to use for this query
	 * @param cacheMode The cache mode to use during execution and subsequent result loading
	 * @param readOnly Whether returned entities should be marked as read-only in the session
	 * @param comment Any sql comment to be applied to the query
	 * @param parameterTypes parameter type map
	 * @param callable Does the query string represent a callable object (i.e., proc)
	 *
	 * @deprecated Use {@link NamedSQLQueryDefinitionBuilder} instead.
	 */
	@Deprecated
	public NamedSQLQueryDefinition(
			String name,
			String query,
			String resultSetRef,
			List<String> querySpaces,
			boolean cacheable,
			String cacheRegion,
			Integer timeout,
			Integer fetchSize,
			FlushMode flushMode,
			CacheMode cacheMode,
			boolean readOnly,
			String comment,
			Map parameterTypes,
			boolean callable) {

		this(
				name,
				query,
				cacheable,
				cacheRegion,
				timeout,
				fetchSize,
				flushMode,
				cacheMode,
				readOnly,
				comment,
				parameterTypes,
				null,		// firstResult
				null,		// maxResults
				resultSetRef,
				querySpaces,
				callable,
				null		// queryReturns
		);
	}

	NamedSQLQueryDefinition(
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
			Map parameterTypes,
			Integer firstResult,
			Integer maxResults,
			String resultSetRef,
			List<String> querySpaces,
			boolean callable,
			NativeSQLQueryReturn[] queryReturns) {
		super(
				name,
				query.trim(), /* trim done to workaround stupid oracle bug that cant handle whitespaces beforeQuery a { in a sp */
				cacheable,
				cacheRegion,
				timeout,
				null,		// lockOptions
				fetchSize,
				flushMode,
				cacheMode,
				readOnly,
				comment,
				parameterTypes,
				firstResult,
				maxResults
		);
		this.resultSetRef = resultSetRef;
		this.querySpaces = querySpaces;
		this.callable = callable;
		this.queryReturns = queryReturns;
	}

	public NativeSQLQueryReturn[] getQueryReturns() {
		return queryReturns;
	}

	public List<String> getQuerySpaces() {
		return querySpaces;
	}

	public boolean isCallable() {
		return callable;
	}

	public String getResultSetRef() {
		return resultSetRef;
	}

	@Override
	public NamedSQLQueryDefinition makeCopy(String name) {
		return new NamedSQLQueryDefinition(
				name,
				getQuery(),
				isCacheable(),
				getCacheRegion(),
				getTimeout(),
				getFetchSize(),
				getFlushMode(),
				getCacheMode(),
				isReadOnly(),
				getComment(),
				getParameterTypes(),
				getFirstResult(),
				getMaxResults(),
				getResultSetRef(),
				getQuerySpaces(),
				isCallable(),
				getQueryReturns()
		);
	}

	public void addQueryReturns(NativeSQLQueryReturn[] queryReturnsToAdd) {
		if ( queryReturnsToAdd != null && queryReturnsToAdd.length > 0 ) {
			int initialQueryReturnsLength = 0;
			if ( this.queryReturns != null ) {
				initialQueryReturnsLength = this.queryReturns.length;
			}
			NativeSQLQueryReturn[] allQueryReturns = new NativeSQLQueryReturn[initialQueryReturnsLength + queryReturnsToAdd.length];

			int i = 0;
			for ( i = 0; i < initialQueryReturnsLength; i++ ) {
				allQueryReturns[i] = this.queryReturns[i];
			}

			for ( int j = 0; j < queryReturnsToAdd.length; j++ ) {
				allQueryReturns[i] = queryReturnsToAdd[j];
				i++;
			}

			this.queryReturns = allQueryReturns;
		}
	}
}
