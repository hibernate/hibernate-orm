/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.spi.AbstractNamedQueryDefinition;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
public class NamedHqlQueryDefinitionImpl<E> extends AbstractNamedQueryDefinition<E> implements NamedHqlQueryDefinition<E> {
	private final String hqlString;
	private final Integer firstResult;
	private final Integer maxResults;
	private final Map<String, String> parameterTypes;

	public NamedHqlQueryDefinitionImpl(
			String name,
			@Nullable Class<E> resultType,
			String hqlString,
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
			Map<String,String> parameterTypes,
			Map<String,Object> hints,
			String location) {
		super(
				name,
				resultType,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				lockOptions,
				timeout,
				fetchSize,
				comment,
				hints,
				location
		);
		this.hqlString = hqlString;
		this.firstResult = firstResult;
		this.maxResults = maxResults;
		this.parameterTypes = parameterTypes;
	}

	@Override
	public String getHqlString() {
		return hqlString;
	}

	@Override
	public NamedSqmQueryMemento<E> resolve(SessionFactoryImplementor factory) {
		return new NamedHqlQueryMementoImpl<>(
				getRegistrationName(),
				getResultType(),
				hqlString,
				firstResult,
				maxResults,
				getCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getFlushMode(),
				getReadOnly(),
				getLockOptions(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				parameterTypes,
				getHints()
		);
	}

}
