/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.named.AbstractNamedQueryMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.internal.SqmSelectionQueryImpl;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;
import org.hibernate.query.sqm.tree.SqmStatement;

import org.checkerframework.checker.nullness.qual.Nullable;

public class NamedCriteriaQueryMementoImpl<E> extends AbstractNamedQueryMemento<E>
		implements NamedSqmQueryMemento<E>, Serializable {

	private final SqmStatement<E> sqmStatement;
	private final Integer firstResult;
	private final Integer maxResults;

	private final LockOptions lockOptions;
	private final Map<String, String> parameterTypes;

	public NamedCriteriaQueryMementoImpl(
			String name,
			@Nullable Class<E> resultType,
			SqmStatement<E> sqmStatement,
			Integer firstResult,
			Integer maxResults,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockOptions lockOptions,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String, String> parameterTypes,
			Map<String, Object> hints) {
		super( name, resultType, cacheable, cacheRegion, cacheMode, flushMode, readOnly, timeout, fetchSize, comment, hints );
		this.sqmStatement = sqmStatement;
		this.firstResult = firstResult;
		this.maxResults = maxResults;
		this.lockOptions = lockOptions;
		this.parameterTypes = parameterTypes;
	}


	@Override
	public void validate(QueryEngine queryEngine) {

	}

	@Override
	public <T> SqmQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType) {
		return new QuerySqmImpl<>( this, resultType, session );
	}

	@Override
	public SqmQueryImplementor<E> toQuery(SharedSessionContractImplementor session) {
		return toQuery( session, getResultType() );
	}

	@Override
	public <T> SqmSelectionQuery<T> toSelectionQuery(Class<T> resultType, SharedSessionContractImplementor session) {
		return new SqmSelectionQueryImpl<>( this, resultType, session );
	}

	@Override
	public String getHqlString() {
		return QuerySqmImpl.CRITERIA_HQL_STRING;
	}

	@Override
	public SqmStatement<E> getSqmStatement() {
		return sqmStatement;
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
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public Map<String, String> getParameterTypes() {
		return parameterTypes;
	}

	@Override
	public NamedSqmQueryMemento<E> makeCopy(String name) {
		return new NamedCriteriaQueryMementoImpl<E>(
				name,
				getResultType(),
				sqmStatement,
				firstResult,
				maxResults,
				getCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getFlushMode(),
				getReadOnly(),
				lockOptions,
				getTimeout(),
				getFetchSize(),
				getComment(),
				parameterTypes,
				getHints()
		);
	}

}
