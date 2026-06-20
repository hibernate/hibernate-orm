/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.internal.SelectionQueryImpl;
import org.hibernate.query.named.spi.NamedSqmQueryMemento;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.tree.spi.SqmStatement;

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
	private final @Nullable String entityGraphName;
	private final @Nullable Map<String, String> parameterTypes;

	public HqlSelectionMementoImpl(
			@Nonnull String name,
			@Nonnull String hqlString,
			@Nullable Class<R> resultType,
			@Nullable String entityGraphName,
			@Nullable QueryFlushMode queryFlushMode,
			@Nullable Timeout timeout,
			@Nullable String comment,
			@Nullable Boolean readOnly,
			@Nullable Integer fetchSize,
			@Nullable Integer firstResult,
			@Nullable Integer maxResults,
			@Nullable Boolean cacheable,
			@Nullable CacheMode cacheMode,
			@Nullable String cacheRegion,
			@Nullable LockMode lockMode,
			@Nullable PessimisticLockScope lockScope,
			@Nullable Timeout lockTimeout,
			@Nullable Locking.FollowOn followOnLockingStrategy,
			@Nullable Map<String, String> parameterTypes,
			@Nonnull Map<String, Object> hints) {
		super( name, resultType,
				queryFlushMode, timeout, comment,
				readOnly, fetchSize, firstResult, maxResults,
				cacheable, cacheMode, cacheRegion,
				lockMode, lockScope, lockTimeout, followOnLockingStrategy,
				hints );
		this.hqlString = hqlString;
		this.entityGraphName = StringHelper.nullIfEmpty( entityGraphName );
		this.parameterTypes = parameterTypes;
	}

	public HqlSelectionMementoImpl(@Nonnull String name, @Nonnull HqlSelectionMementoImpl<R> original) {
		super( name, original );
		this.hqlString = original.hqlString;
		this.entityGraphName = original.entityGraphName;
		this.parameterTypes = original.parameterTypes;
	}

	@Nonnull
	@Override
	public String getHqlString() {
		return hqlString;
	}

	@Nonnull
	@Override
	public String getSelectionString() {
		return getHqlString();
	}

	@Nullable
	@Override
	public SqmStatement<R> getSqmStatement() {
		return null;
	}

	@Override
	@Nullable //FIXME: declared @Nonnull by JPA
	public Class< R> getResultType() {
		return queryType;
	}

	@Override
	@Nullable
	public String getEntityGraphName() {
		return entityGraphName;
	}

	@Nullable
	@Override
	public Map<String, String> getAnticipatedParameterTypes() {
		return parameterTypes;
	}

	@Nonnull
	@Override
	public NamedSqmQueryMemento<R> makeCopy(@Nonnull String name) {
		return new HqlSelectionMementoImpl<>( name, this );
	}

	@Override
	public void validate(@Nonnull QueryEngine queryEngine) {
		final var interpretationCache = queryEngine.getInterpretationCache();
		interpretationCache.resolveHqlInterpretation( hqlString, getResultType(), queryEngine.getHqlTranslator() );
	}

	@Nonnull
	@Override
	public SelectionQueryImplementor<R> toSelectionQuery(@Nonnull SharedSessionContractImplementor session) {
		return toSelectionQuery( session, queryType );
	}

	@Nonnull
	@Override
	public <T> SelectionQueryImplementor<T> toSelectionQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<T> javaType) {
		final HqlInterpretation<T> interpretation = QueryHelper.interpretation( this, javaType, session );
		return new SelectionQueryImpl<>( this, interpretation, javaType, session );
	}
}
