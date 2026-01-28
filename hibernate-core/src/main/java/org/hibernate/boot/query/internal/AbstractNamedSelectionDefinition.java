/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

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
			String name, String location,
			FlushMode flushMode, Timeout timeout, String comment,
			Boolean readOnly, Integer fetchSize, Integer firstRow, Integer maxRows,
			Boolean cacheable, String cacheRegion, CacheMode cacheMode,
			LockMode lockMode, PessimisticLockScope lockScope, Timeout lockTimeout, Locking.FollowOn followOnLockingStrategy,
			Map hints) {
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
