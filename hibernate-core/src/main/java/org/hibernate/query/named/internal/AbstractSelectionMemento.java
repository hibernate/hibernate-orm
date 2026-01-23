/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.named.NamedSelectionMemento;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;

import java.util.Map;

import static java.lang.Boolean.TRUE;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSelectionMemento<R>
		extends AbstractQueryMemento<R>
		implements NamedSelectionMemento<R> {
	protected final Boolean readOnly;
	protected final Integer fetchSize;
	protected final Integer firstRow;
	protected final Integer maxRows;
	protected final	Boolean cacheable;
	protected final	CacheMode cacheMode;
	protected final String cacheRegion;
	protected final LockMode lockMode;
	protected final PessimisticLockScope lockScope;
	protected final Timeout lockTimeout;
	protected final Locking.FollowOn followOnLockingStrategy;

	public AbstractSelectionMemento(
			String name, @Nullable Class<R> queryType,
			FlushMode flushMode, Timeout timeout, String comment,
			Boolean readOnly, Integer fetchSize, Integer firstRow, Integer maxRows,
			Boolean cacheable, CacheMode cacheMode, String cacheRegion,
			LockMode lockMode, PessimisticLockScope lockScope, Timeout lockTimeout, Locking.FollowOn followOnLockingStrategy,
			Map<String, Object> hints) {
		super( name, queryType, flushMode, timeout, comment, hints );
		this.readOnly = readOnly;
		this.fetchSize = fetchSize;
		this.firstRow = firstRow;
		this.maxRows = maxRows;
		this.cacheable = cacheable;
		this.cacheMode = cacheMode;
		this.cacheRegion = cacheRegion;
		this.lockMode = lockMode;
		this.lockScope = lockScope;
		this.lockTimeout = lockTimeout;
		this.followOnLockingStrategy = followOnLockingStrategy;
	}

	public AbstractSelectionMemento(String name, AbstractSelectionMemento<R> original) {
		super( name, original );
		this.readOnly = original.readOnly;
		this.fetchSize = original.fetchSize;
		this.firstRow = original.firstRow;
		this.maxRows = original.maxRows;
		this.cacheable = original.cacheable;
		this.cacheMode = original.cacheMode;
		this.cacheRegion = original.cacheRegion;
		this.lockMode = original.lockMode;
		this.lockScope = original.lockScope;
		this.lockTimeout = original.lockTimeout;
		this.followOnLockingStrategy = original.followOnLockingStrategy;
	}

	@Override
	public Class<? extends R> getResultType() {
		return queryType;
	}

	@Override
	public Boolean getReadOnly() {
		return readOnly;
	}

	@Override
	public Integer getFetchSize() {
		return fetchSize;
	}

	@Override
	public Integer getFirstResult() {
		return firstRow;
	}

	@Override
	public Integer getMaxResults() {
		return maxRows;
	}

	@Override
	public Boolean getCacheable() {
		return cacheable;
	}

	@Override
	public CacheMode getCacheMode() {
		return cacheMode;
	}

	@Override
	public String getCacheRegion() {
		return cacheRegion;
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		if ( cacheable == TRUE ) {
			return cacheMode == null ? CacheRetrieveMode.USE : cacheMode.getJpaRetrieveMode();
		}
		return CacheRetrieveMode.BYPASS;
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		if ( cacheable == TRUE ) {
			return cacheMode == null ? CacheStoreMode.USE : cacheMode.getJpaStoreMode();
		}
		return CacheStoreMode.BYPASS;
	}

	@Override
	public LockMode getHibernateLockMode() {
		return lockMode;
	}

	@Override
	public LockModeType getLockMode() {
		return lockMode == null ? LockModeType.NONE : lockMode.toJpaLockMode();
	}

	@Override
	public PessimisticLockScope getPessimisticLockScope() {
		return lockScope == null ? PessimisticLockScope.NORMAL : lockScope;
	}

	@Override
	public Timeout getLockTimeout() {
		return lockTimeout;
	}

	@Override
	public Locking.FollowOn getFollowOnLockingStrategy() {
		return followOnLockingStrategy;
	}

	@Override
	public MutationQueryImplementor<R> toMutationQuery(SharedSessionContractImplementor session) {
		throw new IllegalMutationQueryException( "Not a NamedMutationMemento" );
	}

	@Override
	public <X> MutationQueryImplementor<X> toMutationQuery(SharedSessionContractImplementor session, Class<X> targetType) {
		throw new IllegalMutationQueryException( "Not a NamedMutationMemento" );
	}

	@Override
	public QueryImplementor<R> toQuery(SharedSessionContractImplementor session) {
		return toSelectionQuery( session );
	}

	@Override
	public <X> QueryImplementor<X> toQuery(SharedSessionContractImplementor session, Class<X> javaType) {
		return toSelectionQuery( session, javaType );
	}
}
