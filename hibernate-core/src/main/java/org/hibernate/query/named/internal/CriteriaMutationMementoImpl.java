/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Timeout;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.internal.AbstractSqmQuery;
import org.hibernate.query.internal.MutationQueryImpl;
import org.hibernate.query.named.spi.NamedSqmQueryMemento;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.tree.spi.SqmDmlStatement;
import org.hibernate.query.sqm.tree.spi.SqmStatement;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class CriteriaMutationMementoImpl<T>
		extends AbstractQueryMemento<T>
		implements SqmMutationMemento<T>, Serializable {
	private final SqmDmlStatement<T> mutationAst;

	public CriteriaMutationMementoImpl(
			@Nonnull String name, @Nullable Class<T> queryType,
			@Nonnull SqmDmlStatement<T> mutationAst,
			@Nullable FlushMode flushMode, @Nullable Timeout timeout, @Nullable String comment,
			@Nonnull Map<String, Object> hints) {
		super( name, queryType, flushMode, timeout, comment, hints );
		this.mutationAst = mutationAst;
	}

	public CriteriaMutationMementoImpl(@Nonnull String name, @Nonnull CriteriaMutationMementoImpl<T> original) {
		super( name, original );
		this.mutationAst = original.mutationAst;
	}

	@Nonnull
	@Override
	public String getHqlString() {
		return AbstractSqmQuery.CRITERIA_HQL_STRING;
	}

	@Nonnull
	@Override
	public SqmStatement<T> getSqmStatement() {
		return mutationAst;
	}

	@Nonnull
	@Override
	public Map<String, String> getAnticipatedParameterTypes() {
		return Map.of();
	}

	@Override
	public void validate(@Nonnull QueryEngine queryEngine) {
		// nothing to do
	}

	@Nonnull
	@Override
	public NamedSqmQueryMemento<T> makeCopy(@Nonnull String name) {
		return new CriteriaMutationMementoImpl<>( name, this );
	}

	@Nonnull
	@Override
	public MutationQueryImplementor<T> toMutationQuery(@Nonnull SharedSessionContractImplementor session) {
		return toMutationQuery( session, queryType );
	}

	@Nonnull
	@Override
	public <X> MutationQueryImplementor<X> toMutationQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> targetType) {
		//noinspection unchecked,rawtypes
		return new MutationQueryImpl( this, mutationAst, session );
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
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", getHqlString() );
	}

	@Nonnull
	@Override
	public <X> SelectionQueryImplementor<X> toSelectionQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> javaType) {
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", getHqlString() );
	}
}
