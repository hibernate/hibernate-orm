/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;


import static org.hibernate.Timeouts.NO_WAIT_MILLI;
import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.QUERY;

/**
 * @author chen zhida
 *
 * Notes: Original code of this class is based on PostgreSQLLockingSupport.
 */
public class GaussDBLockingSupport implements LockingSupport, LockingSupport.Metadata, ConnectionLockTimeoutStrategy {
	public static final LockingSupport LOCKING_SUPPORT = new GaussDBLockingSupport();
	private final boolean supportsNoWait;
	private final boolean supportsSkipLocked;

	public GaussDBLockingSupport() {
		this( true, true );
	}

	public GaussDBLockingSupport(boolean supportsNoWait, boolean supportsSkipLocked) {
		this.supportsNoWait = supportsNoWait;
		this.supportsSkipLocked = supportsSkipLocked;
	}

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.TABLE;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return switch ( timeout.milliseconds() ) {
			case NO_WAIT_MILLI -> supportsNoWait ? QUERY : LockTimeoutType.NONE;
			case SKIP_LOCKED_MILLI -> supportsSkipLocked ? QUERY : LockTimeoutType.NONE;
			case WAIT_FOREVER_MILLI -> LockTimeoutType.NONE;
			// we can apply a timeout via the connection
			default -> LockTimeoutType.CONNECTION;
		};
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.UNSUPPORTED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return this;
	}

	@Override
	public Level getSupportedLevel() {
		return Level.NONE;
	}
}
