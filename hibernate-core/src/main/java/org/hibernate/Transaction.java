/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import javax.persistence.EntityTransaction;
import javax.transaction.Synchronization;

import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Defines the contract for abstracting applications from the configured underlying means of transaction management.
 * Allows the application to define units of work, while maintaining abstraction from the underlying transaction
 * implementation (eg. JTA, JDBC).
 * <p/>
 * A transaction is associated with a {@link Session} and is usually initiated by a call to
 * {@link org.hibernate.Session#beginTransaction()}.  A single session might span multiple transactions since
 * the notion of a session (a conversation between the application and the datastore) is of coarser granularity than
 * the notion of a transaction.  However, it is intended that there be at most one uncommitted transaction associated
 * with a particular {@link Session} at any time.
 * <p/>
 * Implementers are not intended to be thread-safe.
 *
 * @author Anton van Straaten
 * @author Steve Ebersole
 */
public interface Transaction extends EntityTransaction {
	/**
	 * Get the current local status of this transaction.
	 * <p/>
	 * This only accounts for the local view of the transaction status.  In other words it does not check the status
	 * of the actual underlying transaction.
	 *
	 * @return The current local status.
	 */
	TransactionStatus getStatus();

	/**
	 * Register a user synchronization callback for this transaction.
	 *
	 * @param synchronization The Synchronization callback to register.
	 *
	 * @throws HibernateException Indicates a problem registering the synchronization.
	 */
	void registerSynchronization(Synchronization synchronization) throws HibernateException;

	/**
	 * Set the transaction timeout for any transaction started by a subsequent call to {@link #begin} on this instance.
	 *
	 * @param seconds The number of seconds before a timeout.
	 */
	void setTimeout(int seconds);

	/**
	 * Retrieve the transaction timeout set for this transaction.  A negative indicates no timeout has been set.
	 *
	 * @return The timeout, in seconds.
	 */
	int getTimeout();

	/**
	 * Make a best effort to mark the underlying transaction for rollback only.
	 */
	default void markRollbackOnly() {
		setRollbackOnly();
	}

}
