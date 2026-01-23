/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.internal.AbstractSqmQuery;
import org.hibernate.query.internal.MutationQueryImpl;
import org.hibernate.query.named.NamedSqmQueryMemento;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmStatement;

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
			String name, @Nullable Class<T> queryType,
			SqmDmlStatement<T> mutationAst,
			FlushMode flushMode, Timeout timeout, String comment,
			Map<String, Object> hints) {
		super( name, queryType, flushMode, timeout, comment, hints );
		this.mutationAst = mutationAst;
	}

	public CriteriaMutationMementoImpl(String name, CriteriaMutationMementoImpl<T> original) {
		super( name, original );
		this.mutationAst = original.mutationAst;
	}

	@Override
	public String getHqlString() {
		return AbstractSqmQuery.CRITERIA_HQL_STRING;
	}

	@Override
	public SqmStatement<T> getSqmStatement() {
		return mutationAst;
	}

	@Override
	public Map<String, String> getAnticipatedParameterTypes() {
		return Map.of();
	}

	@Override
	public void validate(QueryEngine queryEngine) {
		// nothing to do
	}

	@Override
	public NamedSqmQueryMemento<T> makeCopy(String name) {
		return new CriteriaMutationMementoImpl<>( name, this );
	}

	@Override
	public MutationQueryImplementor<T> toMutationQuery(SharedSessionContractImplementor session) {
		return toMutationQuery( session, queryType );
	}

	@Override
	public <X> MutationQueryImplementor<X> toMutationQuery(SharedSessionContractImplementor session, Class<X> targetType) {
		//noinspection unchecked,rawtypes
		return new MutationQueryImpl( this, mutationAst, session );
	}

	@Override
	public QueryImplementor<T> toQuery(SharedSessionContractImplementor session) {
		return toMutationQuery( session, queryType );
	}

	@Override
	public <X> QueryImplementor<X> toQuery(SharedSessionContractImplementor session, Class<X> javaType) {
		return toMutationQuery( session, javaType );
	}

	@Override
	public SelectionQueryImplementor<T> toSelectionQuery(SharedSessionContractImplementor session) {
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", getHqlString() );
	}

	@Override
	public <X> SelectionQueryImplementor<X> toSelectionQuery(SharedSessionContractImplementor session, Class<X> javaType) {
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", getHqlString() );
	}
}
