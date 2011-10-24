/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.transaction.internal.jta;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.TransactionException;

/**
 * Utility for dealing with JTA statuses.
 *
 * @author Steve Ebersole
 */
public class JtaStatusHelper {
	/**
	 * Extract the status code from a {@link UserTransaction}
	 *
	 * @param userTransaction The {@link UserTransaction} from which to extract the status.
	 *
	 * @return The transaction status
	 *
	 * @throws TransactionException If the {@link UserTransaction} reports the status as unknown
	 */
	public static int getStatus(UserTransaction userTransaction) {
		try {
			final int status = userTransaction.getStatus();
			if ( status == Status.STATUS_UNKNOWN ) {
				throw new TransactionException( "UserTransaction reported transaction status as unknown" );
			}
			return status;
		}
		catch ( SystemException se ) {
			throw new TransactionException( "Could not determine transaction status", se );
		}
	}

	/**
	 * Extract the status code from the current {@link javax.transaction.Transaction} associated with the
	 * given {@link TransactionManager}
	 *
	 * @param transactionManager The {@link TransactionManager} from which to extract the status.
	 *
	 * @return The transaction status
	 *
	 * @throws TransactionException If the {@link TransactionManager} reports the status as unknown
	 */
	public static int getStatus(TransactionManager transactionManager) {
		try {
			final int status = transactionManager.getStatus();
			if ( status == Status.STATUS_UNKNOWN ) {
				throw new TransactionException( "TransactionManager reported transaction status as unknwon" );
			}
			return status;
		}
		catch ( SystemException se ) {
			throw new TransactionException( "Could not determine transaction status", se );
		}
	}

	/**
	 * Does the given status code indicate an active transaction?
	 *
	 * @param status The transaction status code to check
	 *
	 * @return True if the code indicates active; false otherwise.
	 */
	public static boolean isActive(int status) {
		return status == Status.STATUS_ACTIVE;
	}

	/**
	 * Does the status code obtained from the given {@link UserTransaction} indicate an active transaction?
	 *
	 * @param userTransaction The {@link UserTransaction} whose status is to be checked
	 *
	 * @return True if the transaction is active; false otherwise.
	 */
	public static boolean isActive(UserTransaction userTransaction) {
		final int status = getStatus( userTransaction );
		return isActive( status );
	}

	/**
	 * Does the status code obtained from the given {@link TransactionManager} indicate an active transaction?
	 *
	 * @param transactionManager The {@link TransactionManager} whose status is to be checked
	 *
	 * @return True if the transaction is active; false otherwise.
	 */
	public static boolean isActive(TransactionManager transactionManager) {
		return isActive( getStatus( transactionManager ) );
	}

	/**
	 * Does the given status code indicate a rolled back transaction?
	 *
	 * @param status The transaction status code to check
	 *
	 * @return True if the code indicates a roll back; false otherwise.
	 */
	public static boolean isRollback(int status) {
		return status == Status.STATUS_MARKED_ROLLBACK ||
				status == Status.STATUS_ROLLING_BACK ||
				status == Status.STATUS_ROLLEDBACK;
	}

	/**
	 * Does the status code obtained from the given {@link UserTransaction} indicate a roll back?
	 *
	 * @param userTransaction The {@link UserTransaction} whose status is to be checked
	 *
	 * @return True if the transaction indicates roll back; false otherwise.
	 */
	public static boolean isRollback(UserTransaction userTransaction) {
		return isRollback( getStatus( userTransaction ) );
	}

	/**
	 * Does the status code obtained from the given {@link TransactionManager} indicate a roll back?
	 *
	 * @param transactionManager The {@link TransactionManager} whose status is to be checked
	 *
	 * @return True if the transaction indicates roll back; false otherwise.
	 */
	public static boolean isRollback(TransactionManager transactionManager) {
		return isRollback( getStatus( transactionManager ) );
	}

	/**
	 * Does the given status code indicate a committed transaction?
	 *
	 * @param status The transaction status code to check
	 *
	 * @return True if the code indicates a roll back; false otherwise.
	 */
	public static boolean isCommitted(int status) {
		return status == Status.STATUS_COMMITTED;
	}

	/**
	 * Does the status code obtained from the given {@link UserTransaction} indicate a commit?
	 *
	 * @param userTransaction The {@link UserTransaction} whose status is to be checked
	 *
	 * @return True if the transaction indicates commit; false otherwise.
	 */
	public static boolean isCommitted(UserTransaction userTransaction) {
		return isCommitted( getStatus( userTransaction ) );
	}

	/**
	 * Does the status code obtained from the given {@link TransactionManager} indicate a commit?
	 *
	 * @param transactionManager The {@link TransactionManager} whose status is to be checked
	 *
	 * @return True if the transaction indicates commit; false otherwise.
	 */
	public static boolean isCommitted(TransactionManager transactionManager) {
		return isCommitted( getStatus( transactionManager ) );
	}

	/**
	 * Does the given status code indicate the transaction has been marked for rollback?
	 *
	 * @param status The transaction status code to check
	 *
	 * @return True if the code indicates a roll back; false otherwise.
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public static boolean isMarkedForRollback(int status) {
		return status == Status.STATUS_MARKED_ROLLBACK;
	}
}
