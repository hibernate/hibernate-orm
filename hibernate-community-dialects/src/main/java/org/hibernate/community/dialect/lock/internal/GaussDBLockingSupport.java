/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.lock.internal;

import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;



/**
 * @author chen zhida
 *
 * Notes: Original code of this class is based on PostgreSQLLockingSupport.
 */
public class GaussDBLockingSupport implements LockingSupport, LockingSupport.Metadata, ConnectionLockTimeoutStrategy {
	public static final LockingSupport LOCKING_SUPPORT = new GaussDBLockingSupport();


	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.TABLE;
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
