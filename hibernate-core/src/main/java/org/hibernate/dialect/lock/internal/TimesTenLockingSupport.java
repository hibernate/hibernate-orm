/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.NONE;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.QUERY;

/**
 * @author Steve Ebersole
 */
public class TimesTenLockingSupport implements LockingSupport, LockingSupport.Metadata {
	public static final TimesTenLockingSupport TIMES_TEN_LOCKING_SUPPORT = new TimesTenLockingSupport();

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return switch ( timeout.milliseconds() ) {
			case SKIP_LOCKED_MILLI, WAIT_FOREVER_MILLI -> NONE;
			default -> QUERY;
		};
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.COLUMN;
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.IDENTIFIED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return ConnectionLockTimeoutStrategy.NONE;
	}
}
