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
import org.hibernate.query.named.NamedNativeQueryMemento;
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
	private final String resultSetMappingName;
	private final Set<String> synchronizationSpaces;

	public NativeSelectionMementoImpl(
			String name, String sqlString,
			@Nullable Class<R> queryType, String resultSetMappingName, Set<String> synchronizationSpaces,
			FlushMode flushMode, Timeout timeout, String comment,
			Boolean readOnly, Integer fetchSize, Integer firstRow, Integer maxRows,
			Boolean cacheable, CacheMode cacheMode, String cacheRegion,
			LockMode lockMode, PessimisticLockScope lockScope, Timeout lockTimeout, Locking.FollowOn followOnLockingStrategy,
			Map<String, Object> hints) {
		super( name, queryType, flushMode, timeout, comment, readOnly, fetchSize, firstRow, maxRows, cacheable,
				cacheMode,
				cacheRegion, lockMode, lockScope, lockTimeout, followOnLockingStrategy, hints );
		this.sqlString = sqlString;
		this.resultSetMappingName = resultSetMappingName;
		this.synchronizationSpaces = synchronizationSpaces;
	}

	public NativeSelectionMementoImpl(String name, NativeSelectionMementoImpl<R> original) {
		super( name, original );
		this.sqlString = original.sqlString;
		this.resultSetMappingName = original.resultSetMappingName;
		this.synchronizationSpaces = original.synchronizationSpaces;
	}

	@Override
	public String getSqlString() {
		return sqlString;
	}

	@Override
	public String getOriginalSqlString() {
		return getSqlString();
	}

	@Override
	public String getSelectionString() {
		return getSqlString();
	}

	@Override
	public String getResultMappingName() {
		return resultSetMappingName;
	}

	@Override
	public Set<String> getQuerySpaces() {
		return synchronizationSpaces;
	}

	@Override
	public String getEntityGraphName() {
		return null;
	}

	@Override
	public NativeSelectionMementoImpl<R> makeCopy(String name) {
		return new NativeSelectionMementoImpl<>( name, this );
	}

	@Override
	public void validate(QueryEngine queryEngine) {
		// nothing to do
	}

	@Override
	public NativeQueryImplementor<R> toSelectionQuery(SharedSessionContractImplementor session) {
		return toSelectionQuery( session, null );
	}

	@Override
	public <X> NativeQueryImplementor<X> toSelectionQuery(SharedSessionContractImplementor session, Class<X> javaType) {
		return new NativeQueryImpl<>( this, javaType, null, session );
	}

	@Override
	public NativeQueryImplementor<R> toQuery(SharedSessionContractImplementor session) {
		return toSelectionQuery( session );
	}

	@Override
	public <T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session, String resultSetMapping) {
		//noinspection unchecked,rawtypes
		return new NativeQueryImpl( this, null, resultSetMapping, session );
	}

	@Override
	public <X> NativeQueryImplementor<X> toQuery(SharedSessionContractImplementor session, Class<X> javaType) {
		return toSelectionQuery( session, javaType );
	}
}
