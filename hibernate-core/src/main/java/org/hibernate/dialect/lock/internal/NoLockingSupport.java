/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

/**
 * @author Steve Ebersole
 */
public class NoLockingSupport implements LockingSupport, LockingSupport.Metadata {
	public static final NoLockingSupport NO_LOCKING_SUPPORT = new NoLockingSupport();

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return ConnectionLockTimeoutStrategy.NONE;
	}

	@Override
	public PessimisticLockStyle getPessimisticLockStyle() {
		return PessimisticLockStyle.NONE;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return LockTimeoutType.NONE;
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.UNSUPPORTED;
	}
}
