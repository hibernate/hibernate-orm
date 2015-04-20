/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate;

import javax.transaction.Synchronization;

import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.LocalStatus;
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
public interface Transaction {

	/**
	 * Begin this transaction.  No-op if the transaction has already been begun.  Note that this is not necessarily
	 * symmetrical since usually multiple calls to {@link #commit} or {@link #rollback} will error.
	 *
	 * @throws HibernateException Indicates a problem beginning the transaction.
	 */
	public void begin();

	/**
	 * Commit this transaction.  This might entail a number of things depending on the context:<ul>
	 *     <li>
	 *         If this transaction is the {@link #isInitiator initiator}, {@link Session#flush} the {@link Session}
	 *         with which it is associated (unless {@link Session} is in {@link FlushMode#MANUAL}).
	 *     </li>
	 *     <li>
	 *         If this transaction is the {@link #isInitiator initiator}, commit the underlying transaction.
	 *     </li>
	 *     <li>
	 *         Coordinate various callbacks
	 *     </li>
	 * </ul>
	 *
	 * @throws HibernateException Indicates a problem committing the transaction.
	 */
	public void commit();

	/**
	 * Rollback this transaction.  Either rolls back the underlying transaction or ensures it cannot later commit
	 * (depending on the actual underlying strategy).
	 *
	 * @throws HibernateException Indicates a problem rolling back the transaction.
	 */
	public void rollback();

	/**
	 * Get the current local status of this transaction.
	 * <p/>
	 * This only accounts for the local view of the transaction status.  In other words it does not check the status
	 * of the actual underlying transaction.
	 *
	 * @return The current local status.
	 */
	public TransactionStatus getStatus();

	/**
	 * Register a user synchronization callback for this transaction.
	 *
	 * @param synchronization The Synchronization callback to register.
	 *
	 * @throws HibernateException Indicates a problem registering the synchronization.
	 */
	public void registerSynchronization(Synchronization synchronization) throws HibernateException;

	/**
	 * Set the transaction timeout for any transaction started by a subsequent call to {@link #begin} on this instance.
	 *
	 * @param seconds The number of seconds before a timeout.
	 */
	public void setTimeout(int seconds);

	/**
	 * Retrieve the transaction timeout set for this transaction.  A negative indicates no timeout has been set.
	 *
	 * @return The timeout, in seconds.
	 */
	public int getTimeout();

	/**
	 * Make a best effort to mark the underlying transaction for rollback only.
	 */
	public void markRollbackOnly();

}
