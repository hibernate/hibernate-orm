/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

import static org.hibernate.Timeouts.NO_WAIT_MILLI;
import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.NONE;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.QUERY;

/**
 * LockingSupport for OracleDialect
 *
 * @author Steve Ebersole
 */
public class OracleLockingSupport implements LockingSupport, LockingSupport.Metadata {
	public static final OracleLockingSupport ORACLE_LOCKING_SUPPORT = new OracleLockingSupport();

	private final boolean supportsNoWait;
	private final boolean supportsSkipLocked;

	public OracleLockingSupport() {
		supportsNoWait = true;
		supportsSkipLocked = true;
	}

	public OracleLockingSupport(DatabaseVersion version) {
		supportsNoWait = version.isSameOrAfter( 9 );
		supportsSkipLocked = version.isSameOrAfter( 10 );
	}

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return switch( timeout.milliseconds() ) {
			case NO_WAIT_MILLI -> supportsNoWait ? QUERY : NONE;
			case SKIP_LOCKED_MILLI -> supportsSkipLocked ? QUERY : NONE;
			case WAIT_FOREVER_MILLI -> NONE;
			default -> QUERY;
		};
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.COLUMN;
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
