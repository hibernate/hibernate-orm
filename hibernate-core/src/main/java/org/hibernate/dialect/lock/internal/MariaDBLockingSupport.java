/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

import static org.hibernate.Timeouts.NO_WAIT_MILLI;
import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;
import static org.hibernate.dialect.lock.internal.MySQLLockingSupport.MYSQL_CONN_LOCK_TIMEOUT_STRATEGY;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.NONE;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.QUERY;

/**
 * LockingSupport for MariaDBDialect
 *
 * @author Steve Ebersole
 */
public class MariaDBLockingSupport implements LockingSupport, LockingSupport.Metadata {
	private final LockTimeoutType skipLockedType;
	private final LockTimeoutType noWaitType;
	private final LockTimeoutType waitType;

	public MariaDBLockingSupport(boolean supportsSkipLocked, boolean supportsNoWait, boolean supportsWait) {
		this.skipLockedType = supportsSkipLocked ? QUERY : NONE;
		this.noWaitType = supportsNoWait ? QUERY : NONE;
		// Real lock timeouts need to be applied on the Connection
		// todo (db-locking) : integrate connection-based lock timeouts.  for now report NONE
		//this.waitType = supportsWait ? CONNECTION : NONE;
		this.waitType = NONE;
	}

	public MariaDBLockingSupport(boolean supportsSkipLocked, boolean supportsWait) {
		this( supportsSkipLocked, supportsWait, supportsWait );
	}

	public MariaDBLockingSupport(DatabaseVersion databaseVersion) {
		this( databaseVersion.isSameOrAfter( 10, 6 ), true, true );
	}

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return switch ( timeout.milliseconds() ) {
			case NO_WAIT_MILLI -> noWaitType;
			case SKIP_LOCKED_MILLI -> skipLockedType;
			case WAIT_FOREVER_MILLI -> NONE;
			default -> waitType;
		};
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.FULL;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return MYSQL_CONN_LOCK_TIMEOUT_STRATEGY;
	}
}
