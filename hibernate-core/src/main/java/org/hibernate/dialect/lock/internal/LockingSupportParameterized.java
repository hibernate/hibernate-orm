/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.PessimisticLockStyle;
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
 * @author Steve Ebersole
 */
public class LockingSupportParameterized implements LockingSupport, LockingSupport.Metadata {
	private final PessimisticLockStyle pessimisticLockStyle;
	private final RowLockStrategy rowLockStrategy;

	private final LockTimeoutType supportsWaitType;
	private final LockTimeoutType supportsNoWaitType;
	private final LockTimeoutType supportsSkipLockedType;

	private final OuterJoinLockingType outerJoinLockingType;

	public LockingSupportParameterized(
			PessimisticLockStyle pessimisticLockStyle,
			RowLockStrategy rowLockStrategy,
			LockTimeoutType waitType,
			LockTimeoutType noWaitType,
			LockTimeoutType skipLockedType,
			OuterJoinLockingType outerJoinLockingType) {
		this.pessimisticLockStyle = pessimisticLockStyle;
		this.rowLockStrategy = rowLockStrategy;
		this.supportsWaitType = waitType;
		this.supportsNoWaitType = noWaitType;
		this.supportsSkipLockedType = skipLockedType;
		this.outerJoinLockingType = outerJoinLockingType;
	}

	public LockingSupportParameterized(
			PessimisticLockStyle pessimisticLockStyle,
			RowLockStrategy rowLockStrategy,
			boolean supportsWait,
			boolean supportsNoWait,
			boolean supportsSkipLocked,
			OuterJoinLockingType outerJoinLockingType) {
		this(
				pessimisticLockStyle,
				rowLockStrategy,
				supportsWait ? QUERY : NONE,
				supportsNoWait ? QUERY : NONE,
				supportsSkipLocked ? QUERY : NONE,
				outerJoinLockingType
		);
	}

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public PessimisticLockStyle getPessimisticLockStyle() {
		return pessimisticLockStyle;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return rowLockStrategy;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return switch ( timeout.milliseconds() ) {
			case SKIP_LOCKED_MILLI -> supportsSkipLockedType;
			case NO_WAIT_MILLI -> supportsNoWaitType;
			case WAIT_FOREVER_MILLI -> QUERY;
			default -> supportsWaitType;
		};
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return outerJoinLockingType;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return ConnectionLockTimeoutStrategy.NONE;
	}
}
