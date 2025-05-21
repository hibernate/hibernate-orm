/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

/**
 * LockingSupport for HANADialect
 *
 * @author Steve Ebersole
 */
public class HANALockingSupport extends LockingSupportParameterized {
	public static final HANALockingSupport HANA_LOCKING_SUPPORT = new HANALockingSupport( true	);

	public HANALockingSupport(boolean supportsSkipLocked) {
		super(
				PessimisticLockStyle.CLAUSE,
				RowLockStrategy.COLUMN,
				false,
				false,
				supportsSkipLocked,
				OuterJoinLockingType.IDENTIFIED
		);
	}

	@Override
	public Metadata getMetadata() {
		return this;
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
