/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.boot.query.NamedSelectionDefinition;

import java.util.Map;

/// Base support for boot-time modeling of [named selection][NamedSelectionDefinition] queries.
///
/// @author Steve Ebersole
public abstract class AbstractNamedSelectionDefinition<R>
		extends AbstractNamedQueryDefinition<R>
		implements NamedSelectionDefinition<R> {
	protected final Boolean readOnly;
	protected final Integer fetchSize;
	protected final Integer firstRow;
	protected final Integer maxRows;

	protected final Boolean cacheable;
	protected final String cacheRegion;
	protected final CacheMode cacheMode;

	protected final LockMode lockMode;
	protected final PessimisticLockScope lockScope;
	protected final Timeout lockTimeout;
	protected final Locking.FollowOn followOnLockingStrategy;

	public AbstractNamedSelectionDefinition(
			@Nonnull String name,
			@Nullable String location,
			@Nullable FlushMode flushMode,
			@Nullable Timeout timeout,
			@Nullable String comment,
			@Nullable Boolean readOnly,
			@Nullable Integer fetchSize,
			@Nullable Integer firstRow,
			@Nullable Integer maxRows,
			@Nullable Boolean cacheable,
			@Nullable String cacheRegion,
			@Nullable CacheMode cacheMode,
			@Nullable LockMode lockMode,
			@Nullable PessimisticLockScope lockScope,
			@Nullable Timeout lockTimeout,
			@Nullable Locking.FollowOn followOnLockingStrategy,
			@Nonnull Map<String, Object> hints) {
		super( name, location, flushMode, timeout, comment, hints );
		this.readOnly = readOnly;
		this.fetchSize = cleanInteger( fetchSize, -1 );
		this.firstRow = cleanInteger( firstRow, -1 );
		this.maxRows = cleanInteger( maxRows, -1 );
		this.cacheable = cacheable;
		this.cacheRegion = cacheRegion;
		this.cacheMode = cacheMode;
		this.lockMode = lockMode;
		this.lockScope = lockScope;
		this.lockTimeout = lockTimeout;
		this.followOnLockingStrategy = followOnLockingStrategy;
	}

	/**
	 * This value would always come from Hibernate annotations.  Hibernate
	 * always exposes timeouts in seconds...
	 */
	protected static Timeout cleanTimeout(int timeoutInSeconds) {
		return switch ( timeoutInSeconds ) {
			case Timeouts.WAIT_FOREVER_MILLI -> null;
			case Timeouts.NO_WAIT_MILLI -> Timeouts.NO_WAIT;
			case Timeouts.SKIP_LOCKED_MILLI -> Timeouts.SKIP_LOCKED;
			default -> Timeout.seconds( timeoutInSeconds );
		};
	}

	protected static Integer cleanInteger(Integer value, int defaultMatch) {
		return cleanInteger( value, defaultMatch, null );
	}

	protected static Integer cleanInteger(Integer value, int defaultMatch, Integer defaultValue) {
		return value != null && value == defaultMatch
				? defaultValue
				: value;
	}

	@Override
	public Boolean getReadOnly() {
		return readOnly;
	}

	@Override
	public Boolean getCacheable() {
		return cacheable;
	}

	@Override
	public String getCacheRegion() {
		return cacheRegion;
	}

	@Override
	public CacheMode getCacheMode() {
		return cacheMode;
	}

	@Override
	public LockMode getHibernateLockMode() {
		return lockMode;
	}

	@Override
	public PessimisticLockScope getLockScope() {
		return lockScope;
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
	public Integer getFetchSize() {
		return fetchSize;
	}
}
