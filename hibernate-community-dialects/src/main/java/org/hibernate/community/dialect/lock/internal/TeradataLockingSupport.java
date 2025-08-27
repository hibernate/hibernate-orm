/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.Timeouts;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

/**
 * @author Steve Ebersole
 */
public class TeradataLockingSupport implements LockingSupport, LockingSupport.Metadata {
	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		if ( timeout.milliseconds() == Timeouts.NO_WAIT_MILLI ) {
			return LockTimeoutType.QUERY;
		}
		// todo (db-locking) : maybe getConnectionLockTimeoutStrategy?
		return LockTimeoutType.NONE;
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.UNSUPPORTED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		// todo (db-locking) : not sure about this for Teradata...
		return ConnectionLockTimeoutStrategy.NONE;
	}
}
