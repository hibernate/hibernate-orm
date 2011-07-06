/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
 */
public class NamedSQLQueryDefinition extends NamedQueryDefinition {

	private NativeSQLQueryReturn[] queryReturns;
	private final List<String> querySpaces;
	private final boolean callable;
	private String resultSetRef;

	/**
	 * This form used to construct a NamedSQLQueryDefinition from the binder
	 * code when a the result-set mapping information is explicitly
	 * provided in the query definition (i.e., no resultset-mapping used)
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
	 */
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
		super(
				name,
				query.trim(), /* trim done to workaround stupid oracle bug that cant handle whitespaces before a { in a sp */
				cacheable,
				cacheRegion,
				timeout,
				fetchSize,
				flushMode,
				cacheMode,
				readOnly,
				comment,
				parameterTypes
		);
		this.queryReturns = queryReturns;
		this.querySpaces = querySpaces;
		this.callable = callable;
	}

	/**
	 * This form used to construct a NamedSQLQueryDefinition from the binder
	 * code when a resultset-mapping reference is used.
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
	 */
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
		super(
				name,
				query.trim(), /* trim done to workaround stupid oracle bug that cant handle whitespaces before a { in a sp */
				cacheable,
				cacheRegion,
				timeout,
				fetchSize,
				flushMode,
				cacheMode,
				readOnly,
				comment,
				parameterTypes
		);
		this.resultSetRef = resultSetRef;
		this.querySpaces = querySpaces;
		this.callable = callable;
	}

	/**
	 * This form used from annotations (?).  Essentially the same as the above using a
	 * resultset-mapping reference, but without cacheMode, readOnly, and comment.
	 *
	 * FIXME: annotations do not use it, so it can be remove from my POV
	 * @deprecated
	 *
	 *
	 * @param query The sql query string
	 * @param resultSetRef The result-set-mapping name
	 * @param querySpaces Any specified query spaces (used for auto-flushing)
	 * @param cacheable Whether the query results are cacheable
	 * @param cacheRegion If cacheable, the region into which to store the results
	 * @param timeout A JDBC-level timeout to be applied
	 * @param fetchSize A JDBC-level fetch-size to be applied
	 * @param flushMode The flush mode to use for this query
	 * @param parameterTypes parameter type map
	 * @param callable Does the query string represent a callable object (i.e., proc)
	 */
	public NamedSQLQueryDefinition(
			String query,
			String resultSetRef,
			List<String> querySpaces,
			boolean cacheable,
			String cacheRegion,
			Integer timeout,
			Integer fetchSize,
			FlushMode flushMode,
			Map parameterTypes,
			boolean callable) {
		this(
				null,
				query,
				resultSetRef,
				querySpaces,
				cacheable,
				cacheRegion,
				timeout,
				fetchSize,
				flushMode,
				null,
				false,
				null,
				parameterTypes,
				callable
		);
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
}