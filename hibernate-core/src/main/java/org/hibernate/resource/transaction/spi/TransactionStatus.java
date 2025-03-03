/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.spi;

/**
 * Enumeration of statuses in which a transaction facade ({@link org.hibernate.Transaction}) might be.
 *
 * @author Andrea Boriero
 */
public enum TransactionStatus {
	/**
	 * The transaction has not yet been started.
	 */
	NOT_ACTIVE,
	/**
	 * The transaction has been started, but not yet completed.
	 */
	ACTIVE,
	/**
	 * The transaction has been completed successfully.
	 */
	COMMITTED,
	/**
	 * The transaction has been rolled back.
	 */
	ROLLED_BACK,
	/**
	 * The transaction has been marked for rollback only.
	 */
	MARKED_ROLLBACK,
	/**
	 * The transaction attempted to commit, but failed.
	 */
	FAILED_COMMIT,
	/**
	 * The transaction attempted to rollback, but failed.
	 */
	FAILED_ROLLBACK,
	/**
	 * Status code indicating a transaction that has begun the second
	 * phase of the two-phase commit protocol, but not yet completed
	 * this phase.
	 */
	COMMITTING,
	/**
	 *  Status code indicating a transaction that is in the process of
	 *  rolling back.
	 */
	ROLLING_BACK;

	public boolean isActive() {
		return this == ACTIVE
			|| this == MARKED_ROLLBACK;
	}

	public boolean isOneOf(TransactionStatus... statuses) {
		for ( TransactionStatus status : statuses ) {
			if ( this == status ) {
				return true;
			}
		}
		return false;
	}

	public boolean isNotOneOf(TransactionStatus... statuses) {
		return !isOneOf( statuses );
	}

	public boolean canRollback() {
		return this == ACTIVE
			|| this == FAILED_COMMIT
			|| this == MARKED_ROLLBACK;
	}
}
