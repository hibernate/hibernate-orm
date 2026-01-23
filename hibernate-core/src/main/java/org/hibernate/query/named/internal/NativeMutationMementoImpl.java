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
import org.hibernate.query.named.NamedMutationMemento;
import org.hibernate.query.named.NamedNativeQueryMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.query.sql.spi.NativeQueryImplementor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class NativeMutationMementoImpl<T>
		extends AbstractQueryMemento<T>
		implements NamedMutationMemento<T>, NamedNativeQueryMemento<T>, Serializable {
	private final String sqlString;
	private final Set<String> synchronizationSpaces;

	public NativeMutationMementoImpl(
			String name, String sqlString, @Nullable Class<T> queryType,
			FlushMode flushMode, Timeout timeout, String comment,
			Map<String, Object> hints,
			Set<String> synchronizationSpaces) {
		super( name, queryType, flushMode, timeout, comment, hints );
		this.sqlString = sqlString;
		this.synchronizationSpaces = synchronizationSpaces;
	}

	public NativeMutationMementoImpl(String name, NativeMutationMementoImpl<T> original) {
		super( name, original );
		this.sqlString = original.sqlString;
		this.synchronizationSpaces = original.synchronizationSpaces;
	}

	@Override
	public String getSqlString() {
		return sqlString;
	}

	@Override
	public Set<String> getQuerySpaces() {
		return synchronizationSpaces;
	}

	@Override
	public NativeQueryImplementor<T> toMutationQuery(SharedSessionContractImplementor session) {
		return toMutationQuery( session, queryType );
	}

	@Override
	public <X> NativeQueryImplementor<X> toMutationQuery(SharedSessionContractImplementor session, Class<X> targetType) {
		//noinspection unchecked,rawtypes
		return new NativeQueryImpl( this, session );
	}

	@Override
	public NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session) {
		return toMutationQuery( session );
	}

	@Override
	public <X> NativeQueryImplementor<X> toQuery(SharedSessionContractImplementor session, Class<X> resultType) {
		return toMutationQuery( session, resultType );
	}

	@Override
	public <X> NativeQueryImplementor<X> toQuery(SharedSessionContractImplementor session, String resultSetMapping) {
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", sqlString );
	}

	@Override
	public void validate(QueryEngine queryEngine) {
		// nothing to do
	}

	@Override
	public NamedNativeQueryMemento<T> makeCopy(String name) {
		return new NativeMutationMementoImpl<>( name, this );
	}

	@Override
	public SelectionQueryImplementor<T> toSelectionQuery(SharedSessionContractImplementor session) {
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", sqlString );
	}

	@Override
	public <X> SelectionQueryImplementor<X> toSelectionQuery(SharedSessionContractImplementor session, Class<X> javaType) {
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", sqlString );
	}



	@Override
	public String getResultMappingName() {
		return null;
	}

	@Override
	public Integer getFirstResult() {
		return -1;
	}

	@Override
	public Integer getMaxResults() {
		return -1;
	}
}
