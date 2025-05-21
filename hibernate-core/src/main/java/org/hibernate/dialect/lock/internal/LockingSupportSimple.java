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

/**
 * LockingSupport implementation based on the legacy, standard
 * implementations from Dialect
 *
 * @author Steve Ebersole
 */
public class LockingSupportSimple implements LockingSupport, LockingSupport.Metadata {
	/**
	 * The support as used to be defined on Dialect itself...
	 */
	public static final LockingSupport STANDARD_SUPPORT = new LockingSupportSimple(
			PessimisticLockStyle.CLAUSE,
			LockTimeoutType.QUERY,
			OuterJoinLockingType.FULL,
			ConnectionLockTimeoutStrategy.NONE
	);

	/**
	 * {@linkplain #STANDARD_SUPPORT Standard support}, expect that locking outer-joins is
	 * not supported.
	 */
	public static final LockingSupport NO_OUTER_JOIN = new LockingSupportSimple(
			PessimisticLockStyle.CLAUSE,
			LockTimeoutType.QUERY,
			OuterJoinLockingType.UNSUPPORTED,
			ConnectionLockTimeoutStrategy.NONE
	);

	private final PessimisticLockStyle lockStyle;
	private final RowLockStrategy rowLockStrategy;
	private final LockTimeoutType lockTimeoutType;
	private final OuterJoinLockingType joinLockingType;
	private final ConnectionLockTimeoutStrategy connectionStrategy;

	public LockingSupportSimple(
			PessimisticLockStyle lockStyle,
			LockTimeoutType lockTimeoutType,
			OuterJoinLockingType joinLockingType,
			ConnectionLockTimeoutStrategy connectionStrategy) {
		this( lockStyle, RowLockStrategy.NONE, lockTimeoutType, joinLockingType, connectionStrategy );
	}

	public LockingSupportSimple(
			PessimisticLockStyle lockStyle,
			RowLockStrategy rowLockStrategy,
			LockTimeoutType lockTimeoutType,
			OuterJoinLockingType joinLockingType,
			ConnectionLockTimeoutStrategy connectionStrategy) {
		this.lockStyle = lockStyle;
		this.rowLockStrategy = rowLockStrategy;
		this.lockTimeoutType = lockTimeoutType;
		this.joinLockingType = joinLockingType;
		this.connectionStrategy = connectionStrategy;
	}

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public PessimisticLockStyle getPessimisticLockStyle() {
		return lockStyle;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return rowLockStrategy;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return lockTimeoutType;
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return joinLockingType;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return connectionStrategy;
	}
}
