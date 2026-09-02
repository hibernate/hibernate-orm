/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.Timeouts;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.FollowOnLockingPolicy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.RowLockStrategy;

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
public class OracleLockingSupport implements LockingSupport, LockingSupport.Metadata, LockingClauseRenderer {
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
	public LockingClauseRenderer getLockingClauseRenderer() {
		return this;
	}

	@Override
	public FollowOnLockingPolicy getFollowOnLockingPolicy() {
		return request -> {
			final var shape = request.statementShape();
			final var pagination = request.pagination();
			return shape.distinct()
					|| shape.grouped()
					|| shape.setOperation()
					|| pagination.limited() && ( shape.ordered() || pagination.offset() );
		};
	}

	@Override
	public String render(LockingClauseRequest request) {
		final StringBuilder fragment = new StringBuilder( " for update" );
		if ( !request.targets().isEmpty() ) {
			fragment.append( " of " );
			LockingClauseRendererSupport.appendTargets( fragment, request.targets() );
		}

		switch ( request.timeout().milliseconds() ) {
			case NO_WAIT_MILLI -> {
				if ( supportsNoWait ) {
					fragment.append( " nowait" );
				}
			}
			case SKIP_LOCKED_MILLI -> {
				if ( supportsSkipLocked ) {
					fragment.append( " skip locked" );
				}
			}
			case WAIT_FOREVER_MILLI -> {
			}
			default -> fragment.append( " wait " ).append( Timeouts.getTimeoutInSeconds( request.timeout() ) );
		}
		return fragment.toString();
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
		// Per Loic, as of 23 at least, Oracle does support this.
		// Let's see what CI says for previous supported versions.
		return OuterJoinLockingType.IDENTIFIED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return ConnectionLockTimeoutStrategy.NONE;
	}
}
