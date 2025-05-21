/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

/**
 * LockingSupport for H2Dialect
 *
 * @author Steve Ebersole
 */
public class H2LockingSupport implements LockingSupport, LockingSupport.Metadata {
	public static final H2LockingSupport H2_LOCKING_SUPPORT = new H2LockingSupport();

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.IGNORED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		// while we can set the `LOCK_TIMEOUT` setting, there seems to be
		// no corollary to read that value - thus we'd not be able to reset
		// is after
		return ConnectionLockTimeoutStrategy.NONE;
	}

}
