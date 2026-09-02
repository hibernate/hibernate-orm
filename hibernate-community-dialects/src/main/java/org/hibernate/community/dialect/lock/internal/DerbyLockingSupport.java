/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.lock.internal;

import jakarta.persistence.Timeout;

import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;

/// Locking support shared by the current and legacy Derby Dialects.
///
/// The renderer produces the locking portion of the clause. Derby's locking
/// strategy appends the `WITH RS` result-set isolation option separately.
///
/// @author Steve Ebersole
public final class DerbyLockingSupport implements LockingSupport, LockingSupport.Metadata, LockingClauseRenderer {
	public static final DerbyLockingSupport INSTANCE = new DerbyLockingSupport();

	private DerbyLockingSupport() {
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
		return request.lockKind() == PessimisticLockKind.SHARE
				? " for read only"
				: " for update";
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return LockTimeoutType.NONE;
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
