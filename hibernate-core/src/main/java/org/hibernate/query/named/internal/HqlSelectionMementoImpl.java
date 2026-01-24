/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.internal.SelectionQueryImpl;
import org.hibernate.query.named.NamedSqmQueryMemento;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.tree.SqmStatement;

import java.io.Serializable;
import java.util.Map;

/**
 * Definition of a named query, defined in the mapping metadata.
 * Additionally, as of JPA 2.1, named query definition can also come
 * from a compiled query.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class HqlSelectionMementoImpl<R>
		extends AbstractSelectionMemento<R>
		implements SqmSelectionMemento<R>, Serializable {
	private final String hqlString;
	private final String entityGraphName;
	private final Map<String, String> parameterTypes;

	public HqlSelectionMementoImpl(
			String name,
			String hqlString,
			@Nullable Class<R> resultType,
			String entityGraphName,
			FlushMode flushMode,
			Timeout timeout,
			String comment,
			Boolean readOnly,
			Integer fetchSize,
			Integer firstResult,
			Integer maxResults,
			Boolean cacheable,
			CacheMode cacheMode,
			String cacheRegion,
			LockMode lockMode,
			PessimisticLockScope lockScope,
			Timeout lockTimeout,
			Locking.FollowOn followOnLockingStrategy,
			Map<String, String> parameterTypes,
			Map<String, Object> hints) {
		super( name, resultType,
				flushMode, timeout, comment,
				readOnly, fetchSize, firstResult, maxResults,
				cacheable, cacheMode, cacheRegion,
				lockMode, lockScope, lockTimeout, followOnLockingStrategy,
				hints );
		this.hqlString = hqlString;
		this.entityGraphName = StringHelper.nullIfEmpty( entityGraphName );
		this.parameterTypes = parameterTypes;
	}

	public HqlSelectionMementoImpl(String name, HqlSelectionMementoImpl<R> original) {
		super( name, original );
		this.hqlString = original.hqlString;
		this.entityGraphName = original.entityGraphName;
		this.parameterTypes = original.parameterTypes;
	}

	@Override
	public String getHqlString() {
		return hqlString;
	}

	@Override
	public String getSelectionString() {
		return getHqlString();
	}

	@Override
	public SqmStatement<R> getSqmStatement() {
		return null;
	}

	@Override
	public Class<? extends R> getResultType() {
		return queryType;
	}

	@Override
	public String getEntityGraphName() {
		return entityGraphName;
	}

	@Override
	public Map<String, String> getAnticipatedParameterTypes() {
		return parameterTypes;
	}

	@Override
	public NamedSqmQueryMemento<R> makeCopy(String name) {
		return new HqlSelectionMementoImpl<>( name, this );
	}

	@Override
	public void validate(QueryEngine queryEngine) {
		final var interpretationCache = queryEngine.getInterpretationCache();
		interpretationCache.resolveHqlInterpretation( hqlString, getResultType(), queryEngine.getHqlTranslator() );
	}

	@Override
	public SelectionQueryImplementor<R> toSelectionQuery(SharedSessionContractImplementor session) {
		return toSelectionQuery( session, queryType );
	}

	@Override
	public <T> SelectionQueryImplementor<T> toSelectionQuery(SharedSessionContractImplementor session, Class<T> javaType) {
		final HqlInterpretation<T> interpretation = QueryHelper.interpretation( this, javaType, session );
		return new SelectionQueryImpl<>( this, interpretation, javaType, session );
	}
}
