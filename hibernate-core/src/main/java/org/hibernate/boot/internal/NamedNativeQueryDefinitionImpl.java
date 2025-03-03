/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.boot.spi.AbstractNamedQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sql.internal.NamedNativeQueryMementoImpl;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;

import org.checkerframework.checker.nullness.qual.Nullable;


/**
 * @author Steve Ebersole
 */
public class NamedNativeQueryDefinitionImpl<E> extends AbstractNamedQueryDefinition<E> implements NamedNativeQueryDefinition<E> {
	private final String sqlString;
	private final String resultSetMappingName;
	private final Set<String> querySpaces;
	private final Integer firstResult;
	private final Integer maxResults;

	public NamedNativeQueryDefinitionImpl(
			String name,
			@Nullable Class<E> resultType,
			String sqlString,
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
				null,
				timeout,
				fetchSize,
				comment,
				hints,
				location
		);
		this.sqlString = sqlString;
		this.resultSetMappingName = resultSetMappingName;
		this.querySpaces = querySpaces;
		this.firstResult = firstResult;
		this.maxResults = maxResults;
	}

	@Override
	public String getSqlQueryString() {
		return sqlString;
	}

	@Override
	public String getResultSetMappingName() {
		return resultSetMappingName;
	}

	@Override
	public NamedNativeQueryMemento<E> resolve(SessionFactoryImplementor factory) {
		return new NamedNativeQueryMementoImpl<>(
				getRegistrationName(),
				getResultType(),
				sqlString,
				sqlString,
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
				firstResult,
				maxResults,
				getHints()
		);
	}

}
