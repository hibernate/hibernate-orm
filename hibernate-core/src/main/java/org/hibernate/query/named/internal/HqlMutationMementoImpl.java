/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.internal.MutationQueryImpl;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.named.spi.NamedSqmQueryMemento;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.tree.spi.SqmStatement;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class HqlMutationMementoImpl<T>
		extends AbstractQueryMemento<T>
		implements SqmMutationMemento<T>, Serializable {
	private final String hqlString;
	private final @Nullable Map<String, String> parameterTypes;

	public HqlMutationMementoImpl(
			@Nonnull String name, @Nonnull String hqlString, @Nullable Class<T> targetType,
			@Nullable Map<String, String> parameterTypes,
			@Nullable QueryFlushMode queryFlushMode, @Nullable Timeout timeout, @Nullable String comment,
			@Nonnull Map<String, Object> hints) {
		super( name, targetType, queryFlushMode, timeout, comment, hints );
		this.hqlString = hqlString;
		this.parameterTypes = parameterTypes;
	}

	public HqlMutationMementoImpl(@Nonnull String name, @Nonnull HqlMutationMementoImpl<T> original) {
		super( name, original );
		this.hqlString = original.hqlString;
		this.parameterTypes = original.parameterTypes;
	}

	@Nonnull
	@Override
	public String getHqlString() {
		return hqlString;
	}

	@Nullable
	@Override
	public SqmStatement<T> getSqmStatement() {
		return null;
	}

	@Nullable
	@Override
	public Map<String, String> getAnticipatedParameterTypes() {
		return parameterTypes;
	}

	@Nonnull
	@Override
	public NamedSqmQueryMemento<T> makeCopy(@Nonnull String name) {
		return new HqlMutationMementoImpl<>( name, this );
	}

	@Override
	public void validate(@Nonnull QueryEngine queryEngine) {
		final var interpretationCache = queryEngine.getInterpretationCache();
		interpretationCache.resolveHqlInterpretation( hqlString, queryType, queryEngine.getHqlTranslator() );
	}

	@Nonnull
	@Override
	public MutationQueryImplementor<T> toMutationQuery(@Nonnull SharedSessionContractImplementor session) {
		return toMutationQuery( session, queryType );
	}

	@Nonnull
	@Override
	public <X> MutationQueryImplementor<X> toMutationQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> targetType) {
		final HqlInterpretation<X> interpretation = QueryHelper.interpretation( this, targetType, session );
		return new MutationQueryImpl<>( this, interpretation, targetType, session );
	}

	@Nonnull
	@Override
	public QueryImplementor<T> toQuery(@Nonnull SharedSessionContractImplementor session) {
		return toMutationQuery( session, queryType );
	}

	@Nonnull
	@Override
	public <X> QueryImplementor<X> toQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> javaType) {
		return toMutationQuery( session, javaType );
	}

	@Nonnull
	@Override
	public SelectionQueryImplementor<T> toSelectionQuery(@Nonnull SharedSessionContractImplementor session) {
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", hqlString );
	}

	@Nonnull
	@Override
	public <X> SelectionQueryImplementor<X> toSelectionQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> javaType) {
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", hqlString );
	}
}
