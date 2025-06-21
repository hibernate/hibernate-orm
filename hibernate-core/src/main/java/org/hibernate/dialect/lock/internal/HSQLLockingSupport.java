/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

/**
 * LockingSupport for HSQLDialect
 *
 * @author Steve Ebersole
 */
public class HSQLLockingSupport implements LockingSupport, LockingSupport.Metadata {
	public static final HSQLLockingSupport LOCKING_SUPPORT = new HSQLLockingSupport();

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return LockTimeoutType.NONE;
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.IGNORED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return ConnectionLockTimeoutStrategy.NONE;
	}
}
