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
import org.hibernate.query.named.spi.NamedMutationMemento;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;
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
	private final @Nullable Set<String> synchronizationSpaces;

	public NativeMutationMementoImpl(
			@Nonnull String name,
			@Nonnull String sqlString,
			@Nullable Class<T> queryType,
			@Nullable FlushMode flushMode,
			@Nullable Timeout timeout,
			@Nullable String comment,
			@Nonnull Map<String, Object> hints,
			@Nullable Set<String> synchronizationSpaces) {
		super( name, queryType, flushMode, timeout, comment, hints );
		this.sqlString = sqlString;
		this.synchronizationSpaces = synchronizationSpaces;
	}

	public NativeMutationMementoImpl(@Nonnull String name, @Nonnull NativeMutationMementoImpl<T> original) {
		super( name, original );
		this.sqlString = original.sqlString;
		this.synchronizationSpaces = original.synchronizationSpaces;
	}

	@Nonnull
	@Override
	public String getSqlString() {
		return sqlString;
	}

	@Nullable
	@Override
	public Set<String> getQuerySpaces() {
		return synchronizationSpaces;
	}

	@Nonnull
	@Override
	public NativeQueryImplementor<T> toMutationQuery(@Nonnull SharedSessionContractImplementor session) {
		return toMutationQuery( session, queryType );
	}

	@Nonnull
	@Override
	public <X> NativeQueryImplementor<X> toMutationQuery(
			@Nonnull SharedSessionContractImplementor session,
			@Nullable Class<X> targetType) {
		//noinspection unchecked,rawtypes
		return new NativeQueryImpl( this, session );
	}

	@Nonnull
	@Override
	public NativeQueryImplementor<T> toQuery(@Nonnull SharedSessionContractImplementor session) {
		return toMutationQuery( session );
	}

	@Nonnull
	@Override
	public <X> NativeQueryImplementor<X> toQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> resultType) {
		return toMutationQuery( session, resultType );
	}

	@Nonnull
	@Override
	public <X> NativeQueryImplementor<X> toQuery(@Nonnull SharedSessionContractImplementor session, @Nullable String resultSetMapping) {
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", sqlString );
	}

	@Override
	public void validate(@Nonnull QueryEngine queryEngine) {
		// nothing to do
	}

	@Nonnull
	@Override
	public NamedNativeQueryMemento<T> makeCopy(@Nonnull String name) {
		return new NativeMutationMementoImpl<>( name, this );
	}

	@Nonnull
	@Override
	public SelectionQueryImplementor<T> toSelectionQuery(@Nonnull SharedSessionContractImplementor session) {
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", sqlString );
	}

	@Nonnull
	@Override
	public <X> SelectionQueryImplementor<X> toSelectionQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> javaType) {
		throw new IllegalSelectQueryException( "Not a NamedSelectionMemento", sqlString );
	}

	@Nullable
	@Override
	public String getResultMappingName() {
		return null;
	}

	@Nonnull
	@Override
	public Integer getFirstResult() {
		return -1;
	}

	@Nonnull
	@Override
	public Integer getMaxResults() {
		return -1;
	}
}
