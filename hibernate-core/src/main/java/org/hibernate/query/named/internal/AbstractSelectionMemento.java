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
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.named.spi.NamedSelectionMemento;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSelectionMemento<R>
		extends AbstractQueryMemento<R>
		implements NamedSelectionMemento<R> {
	protected final @Nullable Boolean readOnly;
	protected final @Nullable Integer fetchSize;
	protected final @Nullable Integer firstRow;
	protected final @Nullable Integer maxRows;
	protected final @Nullable Boolean cacheable;
	protected final @Nullable CacheMode cacheMode;
	protected final @Nullable String cacheRegion;
	protected final @Nullable LockMode lockMode;
	protected final @Nullable PessimisticLockScope lockScope;
	protected final @Nullable Timeout lockTimeout;
	protected final @Nullable Locking.FollowOn followOnLockingStrategy;

	public AbstractSelectionMemento(
			@Nonnull String name, @Nullable Class<R> queryType,
			@Nullable QueryFlushMode queryFlushMode, @Nullable Timeout timeout, @Nullable String comment,
			@Nullable Boolean readOnly, @Nullable Integer fetchSize, @Nullable Integer firstRow, @Nullable Integer maxRows,
			@Nullable Boolean cacheable, @Nullable CacheMode cacheMode, @Nullable String cacheRegion,
			@Nullable LockMode lockMode, @Nullable PessimisticLockScope lockScope, @Nullable Timeout lockTimeout,
			@Nullable Locking.FollowOn followOnLockingStrategy,
			@Nonnull Map<String, Object> hints) {
		super( name, queryType, queryFlushMode, timeout, comment, hints );
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

	public AbstractSelectionMemento(@Nonnull String name, @Nonnull AbstractSelectionMemento<R> original) {
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
	@Nullable //FIXME: declared @Nonnull by JPA
	public Class<R> getResultType() {
		return queryType;
	}

	@Override
	@Nullable
	public Boolean getReadOnly() {
		return readOnly;
	}

	@Override
	@Nullable
	public Integer getFetchSize() {
		return fetchSize;
	}

	@Override
	@Nullable
	public Integer getFirstResult() {
		return firstRow;
	}

	@Override
	@Nullable
	public Integer getMaxResults() {
		return maxRows;
	}

	@Override
	@Nullable
	public Boolean getCacheable() {
		return cacheable;
	}

	@Override
	@Nullable
	public CacheMode getCacheMode() {
		return cacheMode;
	}

	@Override
	@Nullable
	public String getCacheRegion() {
		return cacheRegion;
	}

	@Override
	@Nullable
	public LockMode getHibernateLockMode() {
		return lockMode;
	}

	@Override
	@Nullable
	public PessimisticLockScope getPessimisticLockScope() {
		return lockScope;
	}

	@Override
	@Nullable
	public Timeout getLockTimeout() {
		return lockTimeout;
	}

	@Override
	@Nullable
	public Locking.FollowOn getFollowOnLockingStrategy() {
		return followOnLockingStrategy;
	}

	@Nonnull
	@Override
	public MutationQueryImplementor<R> toMutationQuery(@Nonnull SharedSessionContractImplementor session) {
		throw new IllegalMutationQueryException( "Not a NamedMutationMemento" );
	}

	@Nonnull
	@Override
	public <X> MutationQueryImplementor<X> toMutationQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> targetType) {
		throw new IllegalMutationQueryException( "Not a NamedMutationMemento" );
	}


	@Nonnull
	@Override
	public QueryImplementor<R> toQuery(@Nonnull SharedSessionContractImplementor session) {
		return toSelectionQuery( session );
	}

	@Nonnull
	@Override
	public <X> QueryImplementor<X> toQuery(@Nonnull SharedSessionContractImplementor session, @Nullable Class<X> javaType) {
		return toSelectionQuery( session, javaType );
	}
}
