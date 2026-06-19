/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.query.sql.spi.NativeQueryImplementor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class NativeSelectionMementoImpl<R>
		extends AbstractSelectionMemento<R>
		implements NamedNativeQueryMemento<R>, Serializable {
	private final String sqlString;
	private final @Nullable String resultSetMappingName;
	private final @Nullable Set<String> synchronizationSpaces;

	public NativeSelectionMementoImpl(
			@Nonnull String name,
			@Nonnull String sqlString,
			@Nullable Class<R> queryType,
			@Nullable String resultSetMappingName,
			@Nullable Set<String> synchronizationSpaces,
			@Nullable FlushMode flushMode,
			@Nullable Timeout timeout,
			@Nullable String comment,
			@Nullable Boolean readOnly,
			@Nullable Integer fetchSize,
			@Nullable Integer firstRow,
			@Nullable Integer maxRows,
			@Nullable Boolean cacheable,
			@Nullable CacheMode cacheMode,
			@Nullable String cacheRegion,
			@Nullable LockMode lockMode,
			@Nullable PessimisticLockScope lockScope,
			@Nullable Timeout lockTimeout,
			@Nullable Locking.FollowOn followOnLockingStrategy,
			@Nonnull Map<String, Object> hints) {
		super( name, queryType, flushMode, timeout, comment, readOnly, fetchSize, firstRow, maxRows, cacheable,
				cacheMode, cacheRegion, lockMode, lockScope, lockTimeout, followOnLockingStrategy, hints );
		this.sqlString = sqlString;
		this.resultSetMappingName = resultSetMappingName;
		this.synchronizationSpaces = synchronizationSpaces;
	}

	public NativeSelectionMementoImpl(@Nonnull String name, @Nonnull NativeSelectionMementoImpl<R> original) {
		super( name, original );
		this.sqlString = original.sqlString;
		this.resultSetMappingName = original.resultSetMappingName;
		this.synchronizationSpaces = original.synchronizationSpaces;
	}

	@Nonnull
	@Override
	public String getSqlString() {
		return sqlString;
	}

	@Nonnull
	@Override
	public String getOriginalSqlString() {
		return getSqlString();
	}

	@Nonnull
	@Override
	public String getSelectionString() {
		return getSqlString();
	}

	@Nullable
	@Override
	public String getResultMappingName() {
		return resultSetMappingName;
	}

	@Nullable
	@Override
	public Set<String> getQuerySpaces() {
		return synchronizationSpaces;
	}

	@Override
	@Nullable
	public String getEntityGraphName() {
		return null;
	}

	@Nonnull
	@Override
	public NativeSelectionMementoImpl<R> makeCopy(@Nonnull String name) {
		return new NativeSelectionMementoImpl<>( name, this );
	}

	@Override
	public void validate(@Nonnull QueryEngine queryEngine) {
		// nothing to do
	}

	@Nonnull
	@Override
	public NativeQueryImplementor<R> toSelectionQuery(@Nonnull SharedSessionContractImplementor session) {
		return toSelectionQuery( session, null );
	}

	@Nonnull
	@Override
	public <X> NativeQueryImplementor<X> toSelectionQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> javaType) {
		return new NativeQueryImpl<>( this, javaType, null, session );
	}

	@Nonnull
	@Override
	public NativeQueryImplementor<R> toQuery(@Nonnull SharedSessionContractImplementor session) {
		return toSelectionQuery( session );
	}

	@Nonnull
	@Override
	public <T> NativeQueryImplementor<T> toQuery(@Nonnull SharedSessionContractImplementor session, @Nullable String resultSetMapping) {
		//noinspection unchecked,rawtypes
		return new NativeQueryImpl( this, null, resultSetMapping, session );
	}

	@Nonnull
	@Override
	public <X> NativeQueryImplementor<X> toQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> javaType) {
		return toSelectionQuery( session, javaType );
	}
}
