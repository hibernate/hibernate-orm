/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

/**
 * LockingSupport for DB2Dialect
 *
 * @author Steve Ebersole
 */
public class DB2LockingSupport extends LockingSupportParameterized {
	/**
	 * Builds a locking-strategy for DB2 LUW.
	 */
	public static DB2LockingSupport forDB2(boolean supportsSkipLocked) {
		return new DB2LockingSupport(
				RowLockStrategy.NONE,
				false,
				false,
				supportsSkipLocked
		);
	}

	/**
	 * Builds a locking-strategy for DB2 iOS.
	 */
	public static DB2LockingSupport forDB2i() {
		return new DB2LockingSupport(
				RowLockStrategy.NONE,
				false,
				false,
				true
		);
	}

	/**
	 * Builds a locking-strategy for DB2 on zOS.
	 */
	public static DB2LockingSupport forDB2z() {
		return new DB2LockingSupport(
				// https://www.ibm.com/docs/en/db2-for-zos/12.0.0?topic=statement-update-clause
				RowLockStrategy.COLUMN,
				false,
				false,
				true
		);
	}

	public DB2LockingSupport(
			RowLockStrategy rowLockStrategy,
			boolean supportsWait,
			boolean supportsNoWait,
			boolean supportsSkipLocked) {
		super(
				PessimisticLockStyle.CLAUSE,
				rowLockStrategy,
				supportsWait,
				supportsNoWait,
				supportsSkipLocked,
				OuterJoinLockingType.FULL
		);
	}
}
