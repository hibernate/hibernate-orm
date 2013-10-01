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
package org.hibernate.engine.transaction.spi;

import java.io.Serializable;
import java.sql.Connection;

import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.transaction.synchronization.spi.SynchronizationCallbackCoordinator;

/**
 * Acts as the coordinator between the Hibernate engine and physical transactions.
 *
 * @author Steve Ebersole
 */
public interface TransactionCoordinator extends Serializable {
	/**
	 * Retrieves the context in which this coordinator operates.
	 *
	 * @return The context of the coordinator
	 */
	public TransactionContext getTransactionContext();

	/**
	 * Retrieves the JDBC coordinator currently operating within this transaction coordinator.
	 *
	 * @return The JDBC coordinator.
	 */
	public JdbcCoordinator getJdbcCoordinator();

	/**
	 * Get the Hibernate transaction facade object currently associated with this coordinator.
	 *
	 * @return The current Hibernate transaction.
	 */
	public TransactionImplementor getTransaction();

	/**
	 * Obtain the {@link javax.transaction.Synchronization} registry associated with this coordinator.
	 *
	 * @return The registry
	 */
	public SynchronizationRegistry getSynchronizationRegistry();

	/**
	 * Adds an observer to the coordinator.
	 * <p/>
	 * Unlike synchronizations added to the {@link #getSynchronizationRegistry() registry}, observers are not to be
	 * cleared on transaction completion.
	 *
	 * @param observer The observer to add.
	 */
	public void addObserver(TransactionObserver observer);

	/**
	 * Removed an observer from the coordinator.
	 *
	 * @param observer The observer to remove.
	 */
	public void removeObserver(TransactionObserver observer);
	
	/**
	 * Can we join to the underlying transaction?
	 *
	 * @return {@literal true} if the underlying transaction can be joined or is already joined; {@literal false}
	 * otherwise.
	 *
	 * @see TransactionFactory#isJoinableJtaTransaction(TransactionCoordinator, TransactionImplementor)
	 */
	public boolean isTransactionJoinable();

	/**
	 * Is the underlying transaction already joined?
	 *
	 * @return {@literal true} if the underlying transaction is already joined; {@literal false} otherwise.
	 */
	public boolean isTransactionJoined();

	/**
	 * Reset the transaction's join status.
	 */
	public void resetJoinStatus();

	/**
	 * Are we "in" an active and joined transaction
	 *
	 * @return {@literal true} if there is currently a transaction in progress; {@literal false} otherwise.
	 */
	public boolean isTransactionInProgress();

	/**
	 * Attempts to register JTA synchronization if possible and needed.
	 */
	public void pulse();

	/**
	 * Close the transaction context, returning any user supplied connection from the underlying JDBC coordinator.
	 *
	 * @return The user supplied connection (if one).
	 */
	public Connection close();

	/**
	 * Performs actions needed after execution of a non-transactional query.
	 *
	 * @param success Was the query successfully performed
	 */
	public void afterNonTransactionalQuery(boolean success);

	public void setRollbackOnly();

	public SynchronizationCallbackCoordinator getSynchronizationCallbackCoordinator();

	public boolean isSynchronizationRegistered();
	public boolean takeOwnership();

	public void afterTransaction(TransactionImplementor hibernateTransaction, int status);

	public void sendAfterTransactionBeginNotifications(TransactionImplementor hibernateTransaction);
	public void sendBeforeTransactionCompletionNotifications(TransactionImplementor hibernateTransaction);
	public void sendAfterTransactionCompletionNotifications(TransactionImplementor hibernateTransaction, int status);

	public boolean isActive();

}
