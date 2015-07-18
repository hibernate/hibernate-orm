/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.spi;

/**
 * Enumeration of statuses in which a transaction facade ({@link org.hibernate.Transaction}) might be.
 *
 * @author Andrea Boriero
 */
public enum TransactionStatus {
	/**
	 * The transaction has not yet been begun
	 */
	NOT_ACTIVE,
	/**
	 * The transaction has been begun, but not yet completed.
	 */
	ACTIVE,
	/**
	 * The transaction has been competed successfully.
	 */
	COMMITTED,
	/**
	 * The transaction has been rolled back.
	 */
	ROLLED_BACK,
	/**
	 * The transaction  has been marked for  rollback only.
	 */
	MARKED_ROLLBACK,
	/**
	 * The transaction attempted to commit, but failed.
	 */
	FAILED_COMMIT,
	/**
	 * Status code indicating a transaction that has begun the second
	 * phase of the two-phase commit protocol, but not yet completed
	 * this phase
	 */
	COMMITTING,
	/**
	 *  Status code indicating a transaction that is in the process of
	 *  rolling back.
	 */
	ROLLING_BACK;

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
		return isOneOf(
				TransactionStatus.ACTIVE,
				TransactionStatus.FAILED_COMMIT,
				TransactionStatus.MARKED_ROLLBACK
		);
	}
}
