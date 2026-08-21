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

import static org.hibernate.dialect.lock.internal.MySQLLockingSupport.MYSQL_CONN_LOCK_TIMEOUT_STRATEGY;

/**
 * @author Steve Ebersole
 */
public class TiDBLockingSupport implements LockingSupport, LockingSupport.Metadata {
	public static final TiDBLockingSupport TIDB_LOCKING_SUPPORT = new TiDBLockingSupport();

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return switch ( timeout.milliseconds() ) {
			case Timeouts.SKIP_LOCKED_MILLI, Timeouts.WAIT_FOREVER_MILLI -> LockTimeoutType.NONE;
			default -> LockTimeoutType.QUERY;
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
