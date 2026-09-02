/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.Timeouts;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.StandardConnectionLockTimeoutStrategies;

/**
 * @author Steve Ebersole
 */
public class TiDBLockingSupport implements LockingSupport, LockingSupport.Metadata, LockingClauseRenderer {
	// Max innodb_lock_wait_timeout in TiDB v8.5.5 is 3600
	public static final ConnectionLockTimeoutStrategy MYSQL_CONN_LOCK_TIMEOUT_STRATEGY =
			StandardConnectionLockTimeoutStrategies.mysql( 3600 );
	public static final TiDBLockingSupport TIDB_LOCKING_SUPPORT = new TiDBLockingSupport();

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
		final StringBuilder fragment = new StringBuilder( " for update" );
		switch ( request.timeout().milliseconds() ) {
			case Timeouts.NO_WAIT_MILLI -> fragment.append( " nowait" );
			case Timeouts.SKIP_LOCKED_MILLI, Timeouts.WAIT_FOREVER_MILLI -> {
			}
			default -> {
				if ( request.lockKind() == PessimisticLockKind.UPDATE ) {
					fragment.append( " wait " ).append( Timeouts.getTimeoutInSeconds( request.timeout() ) );
				}
			}
		}
		return fragment.toString();
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
