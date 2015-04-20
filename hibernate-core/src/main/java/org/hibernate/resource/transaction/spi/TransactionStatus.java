/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) {DATE}, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
	 ROLLING_BACK
}
