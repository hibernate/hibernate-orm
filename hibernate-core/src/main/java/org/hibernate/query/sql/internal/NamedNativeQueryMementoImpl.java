/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.named.AbstractNamedQueryMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;
import org.hibernate.query.sql.spi.NativeQueryImplementor;

/**
 * Keeps details of a named native SQL query
 *
 * @author Max Andersen
 * @author Steve Ebersole
 */
public class NamedNativeQueryMementoImpl<E> extends AbstractNamedQueryMemento<E> implements NamedNativeQueryMemento<E> {
	private final String sqlString;
	private final String originalSqlString;

	private final String resultSetMappingName;

	private final Set<String> querySpaces;

	private final Integer firstResult;

	private final Integer maxResults;

	public NamedNativeQueryMementoImpl(
			String name,
			Class<E> resultClass,
			String sqlString,
			String originalSqlString,
			String resultSetMappingName,
			Set<String> querySpaces,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Integer firstResult,
			Integer maxResults,
			Map<String,Object> hints) {
		super(
				name,
				resultClass,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				timeout,
				fetchSize,
				comment,
				hints
		);
		this.sqlString = sqlString;
		this.originalSqlString = originalSqlString;
		this.resultSetMappingName = resultSetMappingName == null || resultSetMappingName.isEmpty()
				? null
				: resultSetMappingName;
		this.querySpaces = querySpaces;
		this.firstResult = firstResult;
		this.maxResults = maxResults;
	}

	public String getResultSetMappingName() {
		return resultSetMappingName;
	}

	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public String getSqlString() {
		return sqlString;
	}

	@Override
	public String getOriginalSqlString() {
		return originalSqlString;
	}

	@Override
	public String getResultMappingName() {
		return resultSetMappingName;
	}

	@Override
	public Integer getFirstResult() {
		return firstResult;
	}

	@Override
	public Integer getMaxResults() {
		return maxResults;
	}

	@Override
	public NamedNativeQueryMemento<E> makeCopy(String name) {
		return new NamedNativeQueryMementoImpl<>(
				name,
				getResultType(),
				sqlString,
				originalSqlString,
				resultSetMappingName,
				querySpaces,
				getCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getFlushMode(),
				getReadOnly(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				getFirstResult(),
				getMaxResults(),
				getHints()
		);
	}

	@Override
	public void validate(QueryEngine queryEngine) {
		// todo (6.0) : add any validation we want here
	}

	@Override
	public NativeQueryImplementor<E> toQuery(SharedSessionContractImplementor session) {
		return new NativeQueryImpl<>( this, getResultType(), session );
	}

	@Override
	public <T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType) {
		return new NativeQueryImpl<>( this, resultType, session );
	}

	@Override
	public <T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session, String resultSetMappingName) {
		return new NativeQueryImpl<>( this, resultSetMappingName, session );
	}
}
