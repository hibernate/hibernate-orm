/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.lock.internal;

import jakarta.persistence.Timeout;

import org.hibernate.Timeouts;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

/// Versioned Firebird locking-clause support.
///
/// @author Steve Ebersole
public final class FirebirdLockingSupport implements LockingSupport, LockingSupport.Metadata, LockingClauseRenderer {
	private final boolean supportsSkipLocked;

	public FirebirdLockingSupport(DatabaseVersion version) {
		supportsSkipLocked = version.isSameOrAfter( 5 );
	}

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public LockingClauseRenderer getLockingClauseRenderer() {
		return this;
	}

	@Override
	public String render(LockingClauseRequest request) {
		return request.timeout().milliseconds() == Timeouts.SKIP_LOCKED_MILLI && supportsSkipLocked
				? " with lock skip locked"
				: " with lock";
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return timeout.milliseconds() == Timeouts.SKIP_LOCKED_MILLI && supportsSkipLocked
				? LockTimeoutType.QUERY
				: LockTimeoutType.NONE;
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.UNSUPPORTED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return ConnectionLockTimeoutStrategy.NONE;
	}
}
